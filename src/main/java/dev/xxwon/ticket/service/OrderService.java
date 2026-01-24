package dev.xxwon.ticket.service;

import dev.xxwon.ticket.domain.Order;
import dev.xxwon.ticket.domain.OrderRepository;
import dev.xxwon.ticket.domain.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;

    @Transactional
    public Long createOrder(Long userId, Long ticketId) {
        var order = orderRepository.save(Order.builder()
                .userId(userId)
                .ticketId(ticketId)
                .status(OrderStatus.INIT)
                .createdAt(java.time.LocalDateTime.now())
                .build());
        return order.getId();
    }
}
