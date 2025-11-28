package com.example_api.epc.controller;

import com.example_api.epc.service.TicketProcessingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
@CrossOrigin(origins = "*")
public class TicketProcessingController {

    private final TicketProcessingService ticketService;

    public TicketProcessingController(TicketProcessingService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping("/processar")
    public ResponseEntity<?> processarBilhetes() {
        Map<String, Object> result = ticketService.processPendingTickets();
        return ResponseEntity.ok(result);
    }

    // opcional: endpoint para reprocessar 1 ticket específico
    @PostMapping("/processar/{ticketId}")
    public ResponseEntity<?> processarUm(@PathVariable Long ticketId) {
        boolean ok = ticketService.processSingleTicket(ticketId);

        return ResponseEntity.ok(Map.of("processed", ok));
    }
}