package dev.xxwon.ticket.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private Long availableQuantity;

    public Ticket(String title, Long totalQuantity) {
        this.title = title;
        this.availableQuantity = totalQuantity;
    }

    public void decreaseAvailableQuantity() {
        if (availableQuantity <= 0) {
            throw new RuntimeException("No tickets available");
        }
        this.availableQuantity--;
    }
}
