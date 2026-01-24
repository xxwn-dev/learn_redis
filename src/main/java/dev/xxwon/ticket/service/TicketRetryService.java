package dev.xxwon.ticket.service;

import dev.xxwon.ticket.domain.Order;
import dev.xxwon.ticket.domain.OrderRepository;
import dev.xxwon.ticket.domain.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketRetryService {

    private final OrderRepository orderRepository;
    private final OrderAsyncService orderAsyncService;

    @Transactional
    public void retryOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid order ID"));

        try {
            orderAsyncService.sendOrderMessage(order.getUserId(), order.getTicketId(), orderId);
        } catch (Exception e) {
            order.markAsFailed(e.getMessage());
            throw e;
        }

    }
}
