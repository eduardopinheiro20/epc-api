package com.example_api.epc.repository;

import com.example_api.epc.entity.TicketSelection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketSelectionRepository extends JpaRepository<TicketSelection, Long> {
    List<TicketSelection> findByTicketId(Long ticketId);

    List<TicketSelection> findByTicketIdIn(List<Long> ticketIds);
}
