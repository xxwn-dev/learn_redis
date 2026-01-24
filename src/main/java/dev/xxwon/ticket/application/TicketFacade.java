package dev.xxwon.ticket.application;

import dev.xxwon.ticket.service.OrderAsyncService;
import dev.xxwon.ticket.service.OrderService;
import dev.xxwon.ticket.service.RedisStockService;
import dev.xxwon.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class TicketFacade {

    private final RedisStockService redisStockService;
    private final OrderAsyncService orderAsyncService;
    private final TicketService ticketService;
    private final OrderService orderService;

    private boolean isRedisDown = false;
    public boolean isRedisDown() { return isRedisDown;}
    public void setRedisDown(boolean down) {
        this.isRedisDown = down;
    }
    public void purchaseTicket(Long userId, Long ticketId) {

        String key = "ticket:" + ticketId;

        if(isRedisDown) {
            fallbackPurchase(userId, ticketId, "Redis is down.");
            return;
        }

        try {
            redisStockService.decreaseStock(key, userId);
        } catch (Exception e) {
            log.error("Redis is failed! Fallback to DB stock management. Error: {}", e.getMessage());
            isRedisDown = true;
            fallbackPurchase(userId, ticketId, e.getMessage());
        }

        if(!isRedisDown) {
            Long orderId = orderService.createOrder(userId, ticketId);
            orderAsyncService.sendOrderMessage(userId, ticketId, orderId);
        }
    }

    private void fallbackPurchase(Long userId, Long ticketId, String reason) {
        log.info("Fallback purchase for user {} and ticket {} due to: {}", userId, ticketId, reason);
        ticketService.purchase(ticketId, userId);
    }
}
