package dev.xxwon.ticket.controller;

import dev.xxwon.ticket.application.TicketFacade;
import dev.xxwon.ticket.service.RedisStockService;
import dev.xxwon.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class TicketController {

    private final TicketFacade ticketFacade;
    private final TicketService ticketService;

    @PostMapping("/purchase/v1/lock/{ticketId}")
    public ResponseEntity<String> purchaseTicket(@PathVariable Long ticketId, @RequestParam Long userId) {
        try {
            String ticketKey = "ticket:" + ticketId;
            ticketService.purchase(ticketId, userId);
            return ResponseEntity.ok("Successfully purchased ticket [" + ticketId + "] for user [" + userId + "].");
        } catch (IllegalStateException e) {
            // 중복 구매나 품절 시 400 Bad Request를 리턴합니다.
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

    }
    @PostMapping("/purchase/v2/redis/{ticketId}")
    public ResponseEntity<String> purchaseTicketRedis(@PathVariable Long ticketId, @RequestParam(required = false, defaultValue = "1") Long userId) {
        try {
            ticketFacade.purchaseTicket(userId, ticketId);
            return ResponseEntity.ok("Successfully purchased ticket [" + ticketId + "] for user [" + userId + "].");
        } catch (IllegalStateException e) {
            // 중복 구매나 품절 시 400 Bad Request를 리턴합니다.
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
