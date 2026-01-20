package dev.xxwon.ticket.service;

import dev.xxwon.ticket.domain.Order;
import dev.xxwon.ticket.domain.OrderRepository;
import dev.xxwon.ticket.domain.Ticket;
import dev.xxwon.ticket.domain.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisTicketService {

    private final StringRedisTemplate redisTemplate;
    private final TicketRepository ticketRepository;
    private final OrderRepository orderRepository;
    private final OrderAsyncService orderAsyncService;

    //티켓 수량 초기화
    public void warmUpStock(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid ticket ID"));

        String key = "ticket:" + ticketId;

        redisTemplate.opsForValue().set(key, String.valueOf(ticket.getAvailableQuantity()));

        redisTemplate.delete(key+":user"); //이전 구매 기록 삭
    }

    public Long purchase(String key, Long userId) {

        String userKey = key + ":user";
        // 1. 중복 확인
        Long addedCount = redisTemplate.opsForSet().add(userKey, userId.toString());
        if(addedCount == null || addedCount == 0L) {
            throw new IllegalStateException("User has already purchased a ticket");
        }
        //2. 재고 감소
        Long remainingTickets = redisTemplate.opsForValue().decrement(key);

        if(remainingTickets != null && remainingTickets < 0) {
            // 티켓이 없으면 다시 수량 복구
            redisTemplate.opsForValue().increment(key);
            redisTemplate.opsForSet().remove(userKey, userId.toString());
            throw new IllegalStateException("Tickets are sold out");
        }

        //3. 비동기 주문 처리
        Long ticketId = Long.parseLong(key.split(":")[1]);
        orderAsyncService.processOrder(userId, ticketId);

        return remainingTickets;
    }
}
