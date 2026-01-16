package dev.xxwon.ticket.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisTicketService {

    private final StringRedisTemplate redisTemplate;

    //티켓 수량 초기화
    public void setTicketCount(String key, Long count) {
        redisTemplate.opsForValue().set(key, count.toString());
    }

    public Long purchase(String key) {
        return redisTemplate.opsForValue().decrement(key);
    }
}
