package dev.xxwon.ticket.controller;

import dev.xxwon.ticket.service.RedisTicketService;
import dev.xxwon.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TicketController {

    private final RedisTicketService redisTicketService;

    @GetMapping("/purchase/{id}")
    public String purchaseTicket(@PathVariable Long ticketId, @RequestParam Long userId) {
        try {
            String ticketKey = "ticket:" + ticketId;
            redisTicketService.purchase(ticketKey, userId);
            return "Ticket [" + ticketId + "] purchased successfully for User [" + userId + "]!";
        } catch (Exception e) {
            return "Error purchasing ticket: " + e.getMessage();
        }

    }
}
