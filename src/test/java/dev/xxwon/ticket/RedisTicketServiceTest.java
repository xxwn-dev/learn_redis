package dev.xxwon.ticket;

import dev.xxwon.ticket.application.TicketFacade;
import dev.xxwon.ticket.config.AsyncConfig;
import dev.xxwon.ticket.domain.OrderRepository;
import dev.xxwon.ticket.domain.Ticket;
import dev.xxwon.ticket.domain.TicketRepository;
import dev.xxwon.ticket.service.RedisStockService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
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

    @AfterEach
    void cleanUp() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
        orderRepository.deleteAllInBatch();
        ticketRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("Facade 이용해 100 개의 재고에 대해 1000번 동시 티켓 구매 시도")
    void redis_concurrency_test() throws InterruptedException {
        //given
        Long totalStock = 100L;
        Ticket ticket = ticketRepository.save(new Ticket("Concert A", totalStock));
        redisStockService.warmUpStock(ticket.getId());

        int threadCount = 1000;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        //when
        for (int i = 0; i < threadCount; i++) {
            long userId = i;
            executorService.submit(() -> {
                try {
                    ticketFacade.purchaseTicket(userId, ticket.getId());
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executorService.shutdown();

        //then
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->{
            long count = orderRepository.count();
            assertThat(count).isEqualTo((long) totalStock);
        });
        long savedOrderCount = orderRepository.count();

        assertThat(savedOrderCount).isEqualTo((long) totalStock);
        String remaining = redisTemplate.opsForValue().get("ticket:" + ticket.getId());
        assertThat(Long.parseLong(remaining)).isEqualTo(0L);
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
