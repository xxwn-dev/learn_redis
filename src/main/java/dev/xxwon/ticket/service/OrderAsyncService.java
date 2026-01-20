package dev.xxwon.ticket.service;

import dev.xxwon.ticket.domain.Order;
import dev.xxwon.ticket.domain.OrderRepository;
import dev.xxwon.ticket.domain.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderAsyncService {

    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;

    @Async("taskExecutor")
    @Transactional
    public void processOrder(Long userId, Long ticketId) {
        //재고 차감
        int result = ticketRepository.decreaseAvailableQuantity(ticketId);
        if(result > 0){
            orderRepository.save(new Order(userId, ticketId));
        }
    }

    @Transactional
    public void saveOrder(Long userId, Long ticketKey) {
        Order order = new Order(userId, ticketKey);
        orderRepository.save(order);
        //save 한 뒤에 재고 차감을 할 경우가 병목지점이 될 수 있음.
    }

}
