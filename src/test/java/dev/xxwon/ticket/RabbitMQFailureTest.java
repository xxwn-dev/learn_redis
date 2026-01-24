package dev.xxwon.ticket;

import dev.xxwon.ticket.application.TicketFacade;
import dev.xxwon.ticket.domain.*;
import dev.xxwon.ticket.service.RedisStockService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
public class RabbitMQFailureTest {

    @Autowired
    private TicketFacade ticketFacade;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RedisStockService redisStockService;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @Test
    @DisplayName("RabbitMQ 장애 상황에서 티켓 구매 시도")
    void rabbitmq_failure_test() {

        Ticket ticket = ticketRepository.save(new Ticket("Concert B", 100L));
        redisStockService.warmUpStock(ticket.getId());
        Long userId = 1L;

        doThrow(new RuntimeException("RabbitMQ is down"))
            .when(rabbitTemplate).convertAndSend(anyString(), anyString(), (Object) any());

        ticketFacade.purchaseTicket(userId, ticket.getId());

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Order order = orderRepository.findAll().stream()
                    .filter(o -> o.getUserId().equals(userId) && o.getTicketId().equals(ticket.getId()))
                    .findFirst()
                    .orElseThrow();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED_AT_PRODUCER);
        });
    }

}
