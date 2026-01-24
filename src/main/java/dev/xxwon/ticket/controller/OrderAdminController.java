package dev.xxwon.ticket.controller;

import dev.xxwon.ticket.domain.Order;
import dev.xxwon.ticket.domain.OrderRepository;
import dev.xxwon.ticket.domain.OrderStatus;
import dev.xxwon.ticket.service.TicketRetryProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class OrderAdminController {

    private final TicketRetryProcessor ticketRetryProcessor;
    private final OrderRepository orderRepository;

    /**
     * 1.실패한 주문 목록 조회 API
     */
    @GetMapping("/failed")
    public ResponseEntity<List<Order>> getFailedOrders() {
        List<Order> failedOrders = orderRepository.findByStatus(OrderStatus.FAILED_AT_PRODUCER);
        return ResponseEntity.ok(failedOrders);
    }

    /**
     * 2. 실패한 주문 재시도 API
     * */
    @PostMapping("/retry")
    public ResponseEntity<String> retryOrders(@RequestBody List<Long> orderIds) {
        ticketRetryProcessor.retryOrders(orderIds);
        return ResponseEntity.ok("select orders retried successfully.");
    }

    @PostMapping("/retry-all")
    public ResponseEntity<String> retryAll(){
        List<Long> failedOrderIds = orderRepository.findByStatus(OrderStatus.FAILED_AT_PRODUCER)
                .stream()
                .map(Order::getId)
                .toList();

        if(failedOrderIds.isEmpty()){
            return ResponseEntity.ok("No failed orders to retry.");
        }

        ticketRetryProcessor.retryOrders(failedOrderIds);
        return ResponseEntity.ok(failedOrderIds.size() + " orders retried successfully.");
    }
}
