package dev.xxwon.ticket.application.outbox;

import dev.xxwon.ticket.config.RabbitConfig;
import dev.xxwon.ticket.domain.OrderCreatedEvent;
import dev.xxwon.ticket.domain.Outbox;
import dev.xxwon.ticket.domain.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private final RabbitTemplate rabbitTemplate;
    private final OutboxRepository outboxRepository;

    @Async("orderRelayExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderEvent(OrderCreatedEvent event) {
        log.info("Relay [Async]: Attempting immediate delivery - Order Id: {}" , event.orderId());
        try {
            String message = String.format("%d:%d:%d", event.userId(), event.ticketId(), event.orderId());
            rabbitTemplate.convertAndSend(RabbitConfig.ORDER_EXCHANGE, RabbitConfig.ORDER_ROUTING_KEY, message);
            outboxRepository.findByAggregateId(event.orderId()).ifPresent(Outbox::markProcessed);

        } catch (Exception e) {
            log.error("Relay [Async]: Failed to send message, Scheduler will retry. OrderId: {}", event.orderId());
        }
    }
}
