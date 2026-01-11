package com.example_api.epc.repository;

import com.example_api.epc.entity.Ticket;
import com.example_api.epc.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByStatus(String status);

    Ticket findBySignature(String pSignature);

    Ticket findFirstByStatusOrderBySavedAtDesc(String pPENDING);

    Page<Ticket> findByBankrollId(
                    Long bankrollId,
                    Pageable pageable
    );

    Page<Ticket> findByBankrollIdAndSavedAtGreaterThanEqual(
                    Long bankrollId,
                    LocalDateTime start,
                    Pageable pageable
    );

    Page<Ticket> findByBankrollIdAndSavedAtLessThanEqual(
                    Long bankrollId,
                    LocalDateTime end,
                    Pageable pageable
    );

    Page<Ticket> findByBankrollIdAndSavedAtBetween(
                    Long bankrollId,
                    LocalDateTime start,
                    LocalDateTime end,
                    Pageable pageable
    );

    @Query("""
        SELECT t FROM Ticket t
        WHERE t.bankroll.user.id = :userId
          AND t.savedAt >= COALESCE(:start, t.savedAt)
          AND t.savedAt <= COALESCE(:end, t.savedAt)
    """)
    Page<Ticket> findByUserIdWithFilters(@Param("userId") Long userId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);

    @Query("""
       select count(t)
       from Ticket t
       where t.user = :user
         and t.status = 'PENDING'
         and t.id in (
            select ts.ticketId
            from TicketSelection ts
            join Fixture f on f.id = ts.fixtureId
            where f.status in ('FT', 'PEN')
         )
    """)
    long countProcessableTickets(@Param("user") User user);

}
