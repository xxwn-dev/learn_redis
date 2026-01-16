package dev.xxwon.ticket.controller;

import dev.xxwon.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @GetMapping("/purchase/{id}")
    public String purchaseTicket(@PathVariable Long id) {
        try {
            ticketService.purchase(id);
            return "Ticket purchased successfully!";
        } catch (Exception e) {
            return "Error purchasing ticket: " + e.getMessage();
        }

    }
}
