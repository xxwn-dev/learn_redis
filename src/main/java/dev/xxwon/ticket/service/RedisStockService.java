package dev.xxwon.ticket.service;

import dev.xxwon.ticket.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedisStockService {

    private final StringRedisTemplate redisTemplate;
    private final TicketRepository ticketRepository;
    private final OrderAsyncService orderAsyncService;
    private final OrderRepository orderRepository;

    //티켓 수량 초기화
    public void warmUpStock(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid ticket ID"));

        String key = "ticket:" + ticketId;
        redisTemplate.opsForValue().set(key, String.valueOf(ticket.getAvailableQuantity()));
    }

    public void decreaseStock(String key, Long userId) {
        String userKey = key + ":user";
        //중복 구매 방지
        Long addedCount = redisTemplate.opsForSet().add(userKey, String.valueOf(userId));
        if(addedCount == null || addedCount == 0L) {
            throw new IllegalStateException("User has already purchased this ticket.");
        }
        //재고 감소
        Long remainingStock = redisTemplate.opsForValue().decrement(key);

        //수량 복구
        if(remainingStock != null && remainingStock < 0){
            redisTemplate.opsForValue().increment(key);
            redisTemplate.opsForSet().remove(userKey, String.valueOf(userId));
            throw new IllegalStateException("Ticket is sold out.");
        }
//        return remainingStock;
    }

    public boolean isAlive() {
        try {
            String pong = redisTemplate.getConnectionFactory().getConnection().ping();
            return "PONG".equals(pong);
        } catch (Exception e) {
            return false;
        }
    }
}
