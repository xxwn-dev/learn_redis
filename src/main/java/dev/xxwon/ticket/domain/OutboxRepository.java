package dev.xxwon.ticket.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {
    Optional<Outbox> findByAggregateId(Long aggregateId);

    long countByStatus(OutboxStatus status);

    List<Outbox> findAllByStatus(OutboxStatus status, Pageable pageable);
}
