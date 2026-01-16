package dev.xxwon.ticket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class RedisConnectionTest {

    @Autowired
    private RedissonClient redissonClient;

    @Test
    @DisplayName("Redisson 연결 테스트")
    void testRedissonConnection() {
        // RedissonClient가 null이 아닌지 확인
        assertThat(redissonClient).isNotNull();

        boolean isShutdown = redissonClient.isShutdown();
        System.out.println("Redisson is shutdown: " + isShutdown);
        assertThat(isShutdown).isFalse();
    }
}
