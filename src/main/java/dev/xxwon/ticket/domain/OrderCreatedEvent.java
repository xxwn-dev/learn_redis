package dev.xxwon.ticket.domain;

public record OrderCreatedEvent(Long userId,
                                Long ticketId,
                                Long orderId) {

}
