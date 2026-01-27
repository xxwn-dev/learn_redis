package dev.xxwon.ticket.service;

import dev.xxwon.ticket.domain.Order;
import dev.xxwon.ticket.domain.OrderRepository;
import dev.xxwon.ticket.domain.OrderStatus;
import dev.xxwon.ticket.domain.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderProcessService {
    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;

    @Transactional
    public void processOrder(String message){
        log.info("consumer transaction start");
        String[] data = message.split(":");
        Long userId = Long.parseLong(data[0]);
        Long ticketId = Long.parseLong(data[1]);
        Long orderId = Long.parseLong(data[2]);

        Order order = orderRepository.findById(orderId).orElse(null);
        if(order != null && order.getStatus() == OrderStatus.SUCCESS){
            log.info("Already processed: {}", orderId);
            return;
        }
        int orderResult = orderRepository.executeUpdateStatus(orderId);

        // 만약 0이라면 아직 DB에 Order가 반영 안 된 것임 -> 리트라이 발생용 예외 던짐
        if (orderResult == 0) {
            throw new RuntimeException("Order not ready yet");
        }

        // 2. 티켓 재고 차감 (DB 수량 차감)
        int ticketResult = ticketRepository.decreaseAvailableQuantity(ticketId);
        if (ticketResult == 0) {
            log.error("Ticket stock exhausted or not found: OrderId{}", orderId);
            return;
        }

    }
}
