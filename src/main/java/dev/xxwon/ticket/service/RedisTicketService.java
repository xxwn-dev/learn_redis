package dev.xxwon.ticket.service;

import dev.xxwon.ticket.domain.Order;
import dev.xxwon.ticket.domain.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisTicketService {

    private final StringRedisTemplate redisTemplate;
    private final OrderRepository orderRepository;
    private final OrderAsyncService orderAsyncService;

    //티켓 수량 초기화
    public void setTicketCount(String key, Long count) {
        redisTemplate.opsForValue().set(key, count.toString());
    }

    public Long purchase(String key, Long userId) {
        //1. Redis에서 티켓 수량 감소
        Long remainingTickets = redisTemplate.opsForValue().decrement(key);

        if(remainingTickets != null && remainingTickets < 0) {
            //2. 티켓이 없으면 다시 수량 복구
            redisTemplate.opsForValue().increment(key);
            throw new IllegalStateException("Tickets are sold out");
        }

        Long ticketId = Long.parseLong(key.split(":")[1]);
        orderAsyncService.processOrder(userId, ticketId);

        return remainingTickets;
    }
}
