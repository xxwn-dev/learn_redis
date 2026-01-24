package dev.xxwon.ticket;

import dev.xxwon.ticket.application.TicketFacade;
import dev.xxwon.ticket.service.RedisStockService;
import dev.xxwon.ticket.service.TicketService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
public class RedisFailureTest {

    @Autowired
    private TicketFacade ticketFacade;

    @MockitoBean
    private RedisStockService redisStockService;

    @MockitoSpyBean
    private TicketService ticketService;

    @Test
    @DisplayName("Redis 장애 복구 스케줄러 테스트")
    void scheduler_recovery_test() {
        Long userId = 1L;
        Long ticketId = 1L;

        doThrow(new RuntimeException("Redis is down"))
            .when(redisStockService).decreaseStock(anyString(), anyLong());

        ticketFacade.purchaseTicket(userId, ticketId);
        assertThat(ticketFacade.isRedisDown()).isTrue();

        doNothing().when(redisStockService).decreaseStock(anyString(), anyLong());
        when(redisStockService.isAlive()).thenReturn(true);

        await().atMost(15, SECONDS).pollInterval(1, SECONDS).untilAsserted(() -> {
            assertThat(ticketFacade.isRedisDown()).isFalse();
        });
        ticketFacade.purchaseTicket(2L, ticketId);

        verify(redisStockService, atLeastOnce()).decreaseStock(anyString(), eq(2L));

    }
}
