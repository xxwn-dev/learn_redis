package dev.xxwon.ticket.controller.test;


import dev.xxwon.ticket.domain.OrderRepository;
import dev.xxwon.ticket.domain.Ticket;
import dev.xxwon.ticket.domain.TicketRepository;
import dev.xxwon.ticket.service.RedisTicketService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
@Profile({"dev", "local"})
@RequiredArgsConstructor
public class TestResetController {
    private final TicketRepository ticketRepository;
    private final RedisTicketService redisTicketService;
    private final OrderRepository orderRepository;

    @PostMapping("/reset/{ticketId}")
    public ResponseEntity<String> reset(@PathVariable Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseGet(() -> {
                    return ticketRepository.save(new Ticket( "Test Concert", 100L));
                });
        ticket.setAvailableQuantity(100L);
        // 2. Redis 재고 초기화 호출
        redisTicketService.warmUpStock(ticketId);

        // 3. 주문 내역 싹 비우기
        orderRepository.deleteAllInBatch();

        return ResponseEntity.ok("Reset Complete for Ticket ID: " + ticketId);
    }
}
