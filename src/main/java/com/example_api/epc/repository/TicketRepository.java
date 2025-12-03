package com.example_api.epc.repository;

import com.example_api.epc.entity.Bankroll;
import com.example_api.epc.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByBankrollAndStatus(Bankroll bankroll, String status);

     List<Ticket> findByStatus(String status);

    Ticket findBySignature(String pSignature);
}
