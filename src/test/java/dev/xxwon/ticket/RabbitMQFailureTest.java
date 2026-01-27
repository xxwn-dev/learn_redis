package dev.xxwon.ticket;

import dev.xxwon.ticket.application.TicketFacade;
import dev.xxwon.ticket.application.outbox.OutboxScheduler;
import dev.xxwon.ticket.domain.*;
import dev.xxwon.ticket.service.RedisStockService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

@SpringBootTest
public class RabbitMQFailureTest {

    @Autowired
    private TicketFacade ticketFacade;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private RedisStockService redisStockService;

    @MockitoSpyBean
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private OutboxScheduler outboxScheduler;

    @Test
    @DisplayName("RabbitMQ 장애시 주문, 아웃박스는 INIT 상태로 유지")
    void rabbitmq_failure_test() {

        //given: 재고 설정 및 유저 준비
        Ticket ticket = ticketRepository.save(new Ticket("Concert B", 100L));
        redisStockService.warmUpStock(ticket.getId());
        Long userId = 1L;

        //stubbing: rabbitmq 메시지 전송시 예외 발생 설정
        doThrow(new RuntimeException("RabbitMQ is down"))
            .when(rabbitTemplate).convertAndSend(anyString(), anyString(), (Object) any());

        //when: 티켓 구매 시도
        try {
            ticketFacade.purchaseTicket(userId, ticket.getId());
        } catch (Exception e) {
            System.out.println("의도된 예외 발생");
        }

        //then: 장애 중에는 INIT
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            // findAll 대신 userId로 정확히 조회
            Order order = orderRepository.findByUserId(userId)
                    .orElseThrow(() -> new NoSuchElementException("주문 데이터가 롤백되었거나 아직 생성 안 됨"));

            assertThat(order.getStatus()).isEqualTo(OrderStatus.INIT);
            Outbox outbox = outboxRepository.findByAggregateId(order.getId()).orElseThrow();
            assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.INIT);
        });


        //recovery: 장애복구
        reset(rabbitTemplate);

        //action: 스케줄러 강제 실행
        outboxScheduler.processedOutbox();

        //final
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Order updatedOrder = orderRepository.findByUserId(userId).orElseThrow();
            assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.SUCCESS);

            Outbox updatedOutbox = outboxRepository.findByAggregateId(updatedOrder.getId()).orElseThrow();
            assertThat(updatedOutbox.getStatus()).isEqualTo(OutboxStatus.PROCESSED);
        });

    }

}
