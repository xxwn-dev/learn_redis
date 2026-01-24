package dev.xxwon.ticket.consumer;

import dev.xxwon.ticket.config.RabbitConfig;
import dev.xxwon.ticket.domain.Order;
import dev.xxwon.ticket.domain.OrderRepository;
import dev.xxwon.ticket.domain.OrderStatus;
import dev.xxwon.ticket.domain.TicketRepository;
import dev.xxwon.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderConsumer {

    private final TicketRepository ticketRepository;
    private final OrderRepository orderRepository;
    private final TicketService ticketService;

    @RabbitListener(queues = RabbitConfig.ORDER_QUEUE)
    @Transactional
    public void recieveOrder(String message) {
        log.info("수신된 메시지: {}", message);

        String[] data = message.split(":");
        Long userId = Long.parseLong(data[0]);
        Long ticketId = Long.parseLong(data[1]);
        Long orderId = Long.parseLong(data[2]);

        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid order ID"));

            if(order.getStatus() == OrderStatus.SUCCESS){
                log.info("Already processed order ID {}. Skipping.", orderId);
                return;
            }

            int result = ticketRepository.decreaseAvailableQuantity(ticketId);

            if(result > 0){
                order.markAsSuccess();
                log.info("Order ID {} processed successfully.", orderId);
            } else {
                order.markAsOutOfStock();
                log.info("Order ID {} failed due to sold out.", orderId);
            }

        } catch (Exception e) {
            log.error("Consumer processing is failed: {}", e.getMessage());
            throw e;
        }
    }
}
