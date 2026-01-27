package dev.xxwon.ticket.consumer;

import dev.xxwon.ticket.config.RabbitConfig;
import dev.xxwon.ticket.domain.Order;
import dev.xxwon.ticket.domain.OrderRepository;
import dev.xxwon.ticket.domain.OrderStatus;
import dev.xxwon.ticket.domain.TicketRepository;
import dev.xxwon.ticket.service.OrderProcessService;
import dev.xxwon.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderConsumer {

    private final TicketRepository ticketRepository;
    private final OrderRepository orderRepository;
    private final TicketService ticketService;
    private final OrderProcessService orderProcessService;

    @RabbitListener(queues = RabbitConfig.ORDER_QUEUE)
    public void recieveOrder(String message) {
        log.info("수신된 메시지: {}", message);

        orderProcessService.processOrder(message);
    }
}
