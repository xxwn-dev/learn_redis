package dev.xxwon.ticket;

import dev.xxwon.ticket.config.AsyncConfig;
import dev.xxwon.ticket.domain.OrderRepository;
import dev.xxwon.ticket.domain.Ticket;
import dev.xxwon.ticket.domain.TicketRepository;
import dev.xxwon.ticket.service.RedisTicketService;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(AsyncConfig.class)
public class RedisTicketServiceTest {

    @Autowired
    private RedisTicketService redisTicketService;

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
    @DisplayName("Redis를 이용해 100 동시 티켓 구매 테스트")
    void redis_concurrency_test() throws InterruptedException {
        //given
        Long totalStock = 100L;
        Ticket ticket = ticketRepository.save(new Ticket("Concert A", totalStock));

        redisTicketService.warmUpStock(ticket.getId());

        int threadCount = 1000;
        try (ExecutorService executorService = Executors.newFixedThreadPool(32)) {
            CountDownLatch latch = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                long userId = i;
                executorService.submit(() -> {
                    try {
                        redisTicketService.purchase("ticket:" + ticket.getId(), userId);
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await();
        }
        Thread.sleep(3000);
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

        redisTicketService.warmUpStock(ticket.getId());

        String ticketKey = "ticket:" + ticket.getId();
        String current = redisTemplate.opsForValue().get(ticketKey);
        Long userId = 999L; //동일한 유저 ID 고정

        //when
        assertDoesNotThrow(() -> {
            redisTicketService.purchase(ticketKey, userId);
        });

        for(int i=0; i<4; i++){
            IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
                redisTicketService.purchase(ticketKey, userId);
            });

            assertThat(exception.getMessage()).contains("User has already purchased a ticket");
        }

        Thread.sleep(500);

        //then
        String remaining = redisTemplate.opsForValue().get(ticketKey);
        assertThat(Long.parseLong(remaining)).isEqualTo(totalStock - 1L);
        assertThat(orderRepository.count()).isEqualTo(1L);
    }
}
