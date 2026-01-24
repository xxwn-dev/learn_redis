package dev.xxwon.ticket.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    boolean existsByTicketIdAndUserId(Long ticketId, Long userId);

    List<Order> findByStatus(OrderStatus status);

    Optional<Order> findByUserIdAndTicketIdAndStatus(Long userId, Long ticketId, OrderStatus status);
}
