package dev.xxwon.ticket.service;

import dev.xxwon.ticket.domain.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final OrderRepository  orderRepository;

    //프로젝트가 시작 될 때 테스트용 티켓 100개 자동 생성
    @PostConstruct
    @Transactional
    public void initTickets() {
//        Ticket ticket = new Ticket("Concert A", 100L);
        if(ticketRepository.count() == 0) {
            ticketRepository.save(new dev.xxwon.ticket.domain.Ticket("Concert A", 100L));
        }
    }

    @Transactional
    public void purchase(Long ticketId, Long userId) {

        //1. 중복 구매 확인
        if(orderRepository.existsByTicketIdAndUserId(ticketId, userId)) {
            throw new IllegalStateException("User has already purchased this ticket");
        }
        //2. DB에서 row lock를 걸고 티켓 조회
        Ticket ticket = ticketRepository.findByIdWithLock(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid ticket ID"));

        //3. 재고 확인 및 감소
        ticket.decreaseAvailableQuantity();

        //4. 주문 저장
        orderRepository.save(Order.builder()
                .userId(userId)
                .ticketId(ticketId)
                .status(OrderStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build());
    }
}



