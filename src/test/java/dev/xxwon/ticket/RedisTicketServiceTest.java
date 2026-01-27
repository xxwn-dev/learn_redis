package dev.xxwon.ticket;

import dev.xxwon.ticket.application.TicketFacade;
import dev.xxwon.ticket.config.AsyncConfig;
import dev.xxwon.ticket.domain.*;
import dev.xxwon.ticket.service.RedisStockService;
import jakarta.persistence.EntityManager;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.junit.jupiter.api.*;

import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Import(AsyncConfig.class)
public class RedisTicketServiceTest {

    @Autowired
    private TicketFacade ticketFacade;

    @Autowired
    private RedisStockService redisStockService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private TicketRepository ticketRepository;
    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private RabbitListenerEndpointRegistry registry;

    @Autowired
    private EntityManager entityManager;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;
    @Autowired
    private RedisConnectionFactory redisFactory;

    @BeforeEach
    void cleanUp() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
        transactionTemplate.execute(status -> {
            entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();

            // 테이블명은 실제 DB 테이블명에 맞춰주세요 (보통 엔티티명의 스네이크 케이스)
            entityManager.createNativeQuery("TRUNCATE TABLE orders").executeUpdate();
            entityManager.createNativeQuery("TRUNCATE TABLE ticket").executeUpdate();
            entityManager.createNativeQuery("TRUNCATE TABLE outbox").executeUpdate();

            entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();
            return null;
        });

    }
    @Autowired
    private ConnectionFactory rabbitConnectionFactory; // CachingConnectionFactory

    @AfterEach
    void tearDown() {
        registry.getListenerContainers().forEach(MessageListenerContainer::stop);

        if(taskScheduler != null) taskScheduler.shutdown();

        // RabbitMQ 연결 강제 종료
//        if (rabbitConnectionFactory instanceof CachingConnectionFactory factory) {
//            factory.destroy();
//        }
//
//        if(redisFactory instanceof LettuceConnectionFactory factory){
//            factory.destroy();
//        }
    }

    @AfterAll
    static void finalTerminate() {
        Thread shutdownThread = new Thread(() -> {
            try {
                Thread.sleep(3000);
                System.out.println("Forcing JVM Exit...");
                System.exit(0);
            } catch (Exception ignored) {}
        });
        shutdownThread.setDaemon(true); // 데몬으로 설정
        shutdownThread.start();
    }

    @Test
    @DisplayName("아웃박스 패턴 적용 후 100개 재고에 대해 1000번 동시 구매 시도")
    void redis_concurrency_test() throws InterruptedException {
        //given
        Long totalStock = 100L;
        Ticket ticket = ticketRepository.save(new Ticket("Concert A", totalStock));
        redisStockService.warmUpStock(ticket.getId());

        int threadCount = 1000;
        ExecutorService executorService = Executors.newFixedThreadPool(32);

        CountDownLatch latch = new CountDownLatch(threadCount);
        try {
            //when
            for (int i = 0; i < threadCount; i++) {
                long userId = i;
                executorService.submit(() -> {
                    try {
                        ticketFacade.purchaseTicket(userId, ticket.getId());
                    } catch (Exception e) {
//                    System.err.println("Purchase failed for user" + userId);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(20, TimeUnit.SECONDS);

            //1. 주문 테이터가 최종 적으로 100개인지
            await().atMost(10, TimeUnit.SECONDS)
                    //.pollInterval(500, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> {
                        long successCount = orderRepository.findByStatus(OrderStatus.SUCCESS).size();
                        System.out.println("Current Success Count: " + successCount);
                        assertThat(successCount).isEqualTo((long) totalStock);

                        //2. 아웃박스 테이블 상태가 Proccessed인지
                        long processedOutboxCount = outboxRepository.countByStatus(OutboxStatus.PROCESSED);
                        assertThat(processedOutboxCount).isEqualTo((long) totalStock);
                    });

            //3. Redis 재고가 0인지
            String remaining = redisTemplate.opsForValue().get("ticket:" + ticket.getId());
            assertThat(Long.parseLong(remaining)).isEqualTo(0L);

        } finally {
            executorService.shutdownNow();
        }
    }
    @Test
    @DisplayName("Redis 티켓 구매 중복 테스트")
    void duplicate_purchase_test() throws InterruptedException {
        //given
        Long totalStock = 100L;
        Ticket ticket = ticketRepository.save(new Ticket("Concert A", totalStock));
        redisStockService.warmUpStock(ticket.getId());
        Long userId = 999L; //동일한 유저 ID 고정


        //when
        assertDoesNotThrow(() -> {
            ticketFacade.purchaseTicket(userId, ticket.getId());
        });

        for(int i=0; i<4; i++){
            assertThrows(IllegalStateException.class, () -> {
                ticketFacade.purchaseTicket(userId, ticket.getId());
            });
        }

        //then
        String remaining = redisTemplate.opsForValue().get("ticket:" + ticket.getId());
        assertThat(Long.parseLong(remaining)).isEqualTo(totalStock - 1L);
        assertThat(orderRepository.count()).isEqualTo(1L);
    }
}
