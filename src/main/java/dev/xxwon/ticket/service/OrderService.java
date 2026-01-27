package dev.xxwon.ticket.service;

import dev.xxwon.ticket.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final StringRedisTemplate redisTemplate;

    @Transactional
    public Long createOrderWithOutbox(Long userId, Long ticketId) {

        var order = orderRepository.save(Order.builder()
                .userId(userId)
                .ticketId(ticketId)
                .status(OrderStatus.INIT)
                .createdAt(LocalDateTime.now())
                .build());

        String payload = userId + ":" + ticketId + ":" + order.getId();
        Outbox outbox = Outbox.builder()
                .aggregateType("ORDER")
                .aggregateId(order.getId())
                .status(OutboxStatus.INIT)
                .payload(payload)
                .build();

        outboxRepository.save(outbox);
        eventPublisher.publishEvent(new OrderCreatedEvent(userId, ticketId, order.getId()));

        return order.getId();
    }

    @Transactional
    public void failOrderAndRollbackStock(Long orderId, Long ticketId){
        orderRepository.findById(orderId).ifPresent(
                order -> order.markAsFailed("message transfer is failed")
        );

        String redisKey = "ticket:" + ticketId;
        redisTemplate.opsForValue().increment(redisKey);

    }
}
