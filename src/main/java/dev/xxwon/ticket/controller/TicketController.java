package dev.xxwon.ticket.controller;

import dev.xxwon.ticket.service.RedisTicketService;
import dev.xxwon.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class TicketController {

    private final RedisTicketService redisTicketService;
    private final TicketService ticketService;

    @PostMapping("/purchase/lock/{ticketId}")
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
    @PostMapping("/purchase/redis/{ticketId}")
    public ResponseEntity<String> purchaseTicketRedis(@PathVariable Long ticketId, @RequestParam(required = false, defaultValue = "1") Long userId) {
        try {
            String ticketKey = "ticket:" + ticketId;
            redisTicketService.purchase(ticketKey, userId);
            return ResponseEntity.ok("Successfully purchased ticket [" + ticketId + "] for user [" + userId + "].");
        } catch (IllegalStateException e) {
            // 중복 구매나 품절 시 400 Bad Request를 리턴합니다.
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

    }
}
