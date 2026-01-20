package dev.xxwon.ticket.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)   //비관적 락 설정
    @Query("select t from Ticket t where t.id = :id")
    Optional<Ticket> findByIdWithLock(Long id);

    @Modifying
    @Query("update Ticket t set t.availableQuantity = t.availableQuantity - 1 where t.id = :id and t.availableQuantity > 0")
    int decreaseAvailableQuantity(Long id);
}
