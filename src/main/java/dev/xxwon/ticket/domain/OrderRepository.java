package dev.xxwon.ticket.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    boolean existsByTicketIdAndUserId(Long ticketId, Long userId);

    List<Order> findByStatus(OrderStatus status);

    Optional<Order> findByUserIdAndTicketId(Long userId, Long ticketId);

    Optional<Order> findByUserId(Long userId);

    @Modifying
    @Query(value = "UPDATE orders o SET o.status = 'SUCCESS' WHERE o.id = :id", nativeQuery = true)
    int executeUpdateStatus(@Param("id") Long id);
}
