package dev.xxwon.ticket.service;

import dev.xxwon.ticket.domain.Order;
import dev.xxwon.ticket.domain.OrderRepository;
import dev.xxwon.ticket.domain.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderAsyncService {

    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;
    private final StringRedisTemplate redisTemplate;

    @Async("taskExecutor")
    @Transactional
    public void processOrder(Long userId, Long ticketId) {

        try {
            //재고 차감
            int result = ticketRepository.decreaseAvailableQuantity(ticketId);
            if(result == 0){
                //재고 부족 시 Redis 롤백 처리(보상 트랜잭션)
                rollbackRedis(userId, ticketId);
                return;
            }
            orderRepository.save(new Order(userId, ticketId));
        } catch (Exception e) {
            rollbackRedis(userId, ticketId);
            log.error("Error processing order for user {} and ticket {}: {}", userId, ticketId, e.getMessage());
        }
    }

    @Transactional
    public void saveOrder(Long userId, Long ticketKey) {
        Order order = new Order(userId, ticketKey);
        orderRepository.save(order);
        //save 한 뒤에 재고 차감을 할 경우가 병목지점이 될 수 있음.
    }

    private void rollbackRedis(Long userId, Long ticketId) {
        String ticketKey = "ticket:" + ticketId;
        String userKey = ticketKey + ":user";
        redisTemplate.opsForValue().increment(ticketKey);
        redisTemplate.opsForSet().remove(userKey, userId.toString());
    }
}
