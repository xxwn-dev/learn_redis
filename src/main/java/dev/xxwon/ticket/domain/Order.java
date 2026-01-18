package dev.xxwon.ticket.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String ticketId;
    private LocalDateTime createdAt;

    public Order(Long userId, String ticketId) {
        this.userId = userId;
        this.ticketId = ticketId;
        this.createdAt = LocalDateTime.now();
    }
}
