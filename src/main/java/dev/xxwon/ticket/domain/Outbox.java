package dev.xxwon.ticket.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Outbox {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String aggregateType;
    private Long aggregateId;
    private String payload;

    @Enumerated(EnumType.STRING)
    private OutboxStatus status;

    public Outbox(String aggregateType, Long aggregateId, String payload){
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.payload = payload;
    }

    public void markProcessed() { this.status = OutboxStatus.PROCESSED; }

    public void markFailed() { this.status = OutboxStatus.FAIL; }
}
