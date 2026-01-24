package dev.xxwon.ticket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketRetryProcessor {

    private final TicketRetryService ticketRetryService;

    public void retryOrders(List<Long> orderIds) {
        for (var id : orderIds) {
            try {
                ticketRetryService.retryOrder(id);
            } catch (Exception e) {
                log.error("Failed to retry order ID {}: {}", id, e.getMessage());
            }
        }
    }
}
