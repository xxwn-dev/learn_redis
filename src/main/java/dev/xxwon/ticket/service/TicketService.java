package dev.xxwon.ticket.service;

import dev.xxwon.ticket.domain.OrderRepository;
import dev.xxwon.ticket.domain.Ticket;
import dev.xxwon.ticket.domain.TicketRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;    //DB 저장용

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
    public void purchase(Long ticketId) {
        //findById 대신 findByIdWithLock 사용
        Ticket ticket = ticketRepository.findByIdWithLock(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid ticket ID"));
        ticket.decreaseAvailableQuantity();
    }
}



