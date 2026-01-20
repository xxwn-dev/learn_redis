package dev.xxwon.ticket;

import dev.xxwon.ticket.config.AsyncConfig;
import dev.xxwon.ticket.domain.OrderRepository;
import dev.xxwon.ticket.domain.TicketRepository;
import dev.xxwon.ticket.service.RedisTicketService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Import(AsyncConfig.class)
public class RedisTicketServiceTest {

    @Autowired
    private RedisTicketService redisTicketService;

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private TicketRepository ticketRepository;

    @Test
    @DisplayName("Redis를 이용해 100 동시 티켓 구매 테스트")
    void redis_concurrency_test() throws InterruptedException {
        //given
        String ticketKey = "ticket:1";
        int totalStock = 100;

        redisTicketService.setTicketCount(ticketKey, (long) totalStock);

        int threadCount = 1000;
        try (ExecutorService executorService = Executors.newFixedThreadPool(32)) {
            CountDownLatch latch = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                long userId = i;
                executorService.submit(() -> {
                    try {
                        redisTicketService.purchase(ticketKey, userId);
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await();
            Thread.sleep(3000);
        }
        long savedOrderCount = orderRepository.count();

        assertThat(savedOrderCount).isEqualTo((long) totalStock);
        assertThat(ticketRepository.findById(1L).get().getAvailableQuantity()).isEqualTo(0);
    }
}
