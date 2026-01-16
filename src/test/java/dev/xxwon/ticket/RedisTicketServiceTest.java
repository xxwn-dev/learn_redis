package dev.xxwon.ticket;

import dev.xxwon.ticket.service.RedisTicketService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
public class RedisTicketServiceTest {

    @Autowired
    private RedisTicketService redisTicketService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    @DisplayName("Redis를 이용해 100 동시 티켓 구매 테스트")
    void redis_concurrency_test() throws InterruptedException {
        //given
        String ticketKey = "ticket:1";
        int threadCount = 100;

        redisTicketService.setTicketCount(ticketKey, 100L);

        try (ExecutorService executorService = Executors.newFixedThreadPool(32)) {
            CountDownLatch latch = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                executorService.submit(() -> {
                    try {
                        redisTicketService.purchase(ticketKey);
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await();
        }
        String remainCount = redisTemplate.opsForValue().get(ticketKey);
        System.out.println("Remaining tickets in Redis: " + remainCount);

        assertThat(remainCount).isEqualTo("0");
    }
}
