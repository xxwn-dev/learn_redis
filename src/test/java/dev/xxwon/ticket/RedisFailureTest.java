package dev.xxwon.ticket;

import dev.xxwon.ticket.application.TicketFacade;
import dev.xxwon.ticket.domain.*;
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

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Test
    @DisplayName("Redis 장애 복구 스케줄러 테스트")
    void scheduler_recovery_test() {
        Long userId = 1L;
        Long ticketId = 1L;

        long initialStock = ticketRepository.findById(ticketId).get().getAvailableQuantity();

        doThrow(new RuntimeException("Redis is down"))
            .when(redisStockService).decreaseStock(anyString(), anyLong());

        ticketFacade.purchaseTicket(userId, ticketId);
        //1번 오더: 장애 플래그가 세워졌는지
        assertThat(ticketFacade.isRedisDown()).isTrue();
        //1번 오더는 동기 방식으로 SUCCESS
        Order order1 = orderRepository.findByUserIdAndTicketId(userId, ticketId).orElseThrow();
        assertThat(order1.getStatus()).isEqualTo(OrderStatus.SUCCESS);

        //Redis 장애 복구 시뮬레이션 및 스케줄러 대기
        doNothing().when(redisStockService).decreaseStock(anyString(), anyLong());
        when(redisStockService.isAlive()).thenReturn(true);

        //스케줄러가 Redis복구 대기
        await().atMost(15, SECONDS).pollInterval(1, SECONDS).untilAsserted(() -> {
            assertThat(ticketFacade.isRedisDown()).isFalse();
        });

        //2번 유저 Redis로 주문
        ticketFacade.purchaseTicket(2L, ticketId);

        //Redis로직 타는지 확인
        verify(redisStockService, atLeastOnce()).decreaseStock(anyString(), eq(2L));

        await().atMost(5, SECONDS)
                .untilAsserted(() -> {
                    Order order2 = orderRepository.findByUserIdAndTicketId(2L, ticketId).orElseThrow();
                    assertThat(order2.getStatus()).isEqualTo(OrderStatus.SUCCESS);

                    //최종 재고 확인
                    Ticket ticket = ticketRepository.findById(ticketId).get();
                    assertThat(ticket.getAvailableQuantity()).isEqualTo(initialStock-2);
                });
    }
}
