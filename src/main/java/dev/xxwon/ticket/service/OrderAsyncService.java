package dev.xxwon.ticket.service;

import dev.xxwon.ticket.config.RabbitConfig;
import dev.xxwon.ticket.domain.Order;
import dev.xxwon.ticket.domain.OrderRepository;
import dev.xxwon.ticket.domain.OrderStatus;
import dev.xxwon.ticket.domain.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderAsyncService {

    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;
    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;


    @Retryable(retryFor = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void sendOrderMessage(Long userId, Long ticketId, Long orderId) {
        try {
            String message =  String.format("%d:%d:%d", userId, ticketId, orderId);
            //order.exchange 로 온 메시지중 order.routing.key 키가 있으면 order.queue로 메시지를 보낸다.
            rabbitTemplate.convertAndSend(RabbitConfig.ORDER_EXCHANGE, RabbitConfig.ORDER_ROUTING_KEY, message);
            log.info("Sent order message for user {} and ticket {}", userId, ticketId);
        } catch (Exception e) {
            log.error("RabbitMQ 전송 실패 (재시도 중...) - User: {}, Ticket: {}", userId, ticketId);
            throw e;
        }
    }

    @Recover
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recoverOrderMessage(Exception e, Long userId, Long ticketId, Long orderId) {
        log.error("retry 3회 실패. Order ID: {}", orderId);
        orderRepository.findById(orderId).ifPresent(order -> {
            order.markAsFailedAtProducer(e.getMessage());
        });
    }

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
