package dev.xxwon.ticket.application;

import dev.xxwon.ticket.service.OrderAsyncService;
import dev.xxwon.ticket.service.OrderService;
import dev.xxwon.ticket.service.RedisStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TicketFacade {

    private final RedisStockService redisStockService;
    private final OrderAsyncService orderAsyncService;
    private final OrderService orderService;

    public void purchaseTicket(Long userId, Long ticketId) {
        String key = "ticket:" + ticketId;

        redisStockService.decreaseStock(key, userId);

        Long orderId = orderService.createOrder(userId, ticketId);

        orderAsyncService.sendOrderMessage(userId, ticketId, orderId);
    }
}
