package com.example_api.epc.repository;

import com.example_api.epc.entity.Bankroll;
import com.example_api.epc.entity.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByBankrollAndStatus(Bankroll bankroll, String status);

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
        WHERE t.bankroll.id = :bankrollId
          AND (:start IS NULL OR t.savedAt >= :start)
          AND (:end IS NULL OR t.savedAt <= :end)
    """)
    Page<Ticket> findByBankrollWithFilters(
                    @Param("bankrollId") Long bankrollId,
                    @Param("start") LocalDateTime start,
                    @Param("end") LocalDateTime end,
                    Pageable pageable
    );

    @Query("""
        SELECT t FROM Ticket t
        LEFT JOIN FETCH t.selections
        WHERE t.bankroll.id = :bankrollId
    """)
        Page<Ticket> findByBankrollWithSelections(
                    @Param("bankrollId") Long bankrollId,
                    Pageable pageable
    );

    @Query("""
        SELECT DISTINCT t FROM Ticket t
        LEFT JOIN FETCH t.selections
        WHERE t.bankroll.id = :bankrollId
          AND (:start IS NULL OR t.savedAt >= :start)
          AND (:end IS NULL OR t.savedAt <= :end)
    """)
    Page<Ticket> findByBankrollWithSelectionsAndFilters(
                    Long bankrollId,
                    LocalDateTime start,
                    LocalDateTime end,
                    Pageable pageable
    );


}
