package dev.xxwon.ticket.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
    boolean existsByTicketIdAndUserId(Long ticketId, Long userId);
}
