package dev.xxwon.ticket.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long ticketId;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(length = 1000)
    private String failureReason;

    private LocalDateTime createdAt;

    public Order(Long userId, Long ticketId) {
        this.userId = userId;
        this.ticketId = ticketId;
        this.createdAt = LocalDateTime.now();
    }

    public void markAsSuccess() {
        this.status = OrderStatus.SUCCESS;
        this.failureReason = null;
    }

    public void markAsFailed(String reason) {
        this.status = OrderStatus.FAILED;
        this.failureReason = reason;
    }

    public void markAsOutOfStock() {
        this.status = OrderStatus.FAILED_NO_STOCK;
        this.failureReason = "No stock available";
    }

    public void markAsFailedAtProducer(String reason) {
        this.status = OrderStatus.FAILED_AT_PRODUCER;
        this.failureReason = reason;
    }
}
