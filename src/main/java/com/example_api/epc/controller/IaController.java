package com.example_api.epc.controller;

import com.example_api.epc.client.IaClient;
import com.example_api.epc.dto.TicketResponse;
import com.example_api.epc.entity.Ticket;
import com.example_api.epc.service.BankrollService;
import com.example_api.epc.service.TicketProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ia")
@CrossOrigin(origins = "http://localhost:4200")
public class IaController {

    @Autowired
    private IaClient iaClient;

    private final TicketProcessingService ticketService;

    private final BankrollService bankrollService;

    public IaController(final TicketProcessingService pTicketService, final BankrollService pBankrollService) {
        ticketService = pTicketService;
        bankrollService = pBankrollService;
    }

    @GetMapping("/bilhete-do-dia")
    public ResponseEntity<?> melhorAposta() {

        TicketResponse response = iaClient.getBilheteDoDia();

        if (!response.isFound()) {
            return ResponseEntity.ok(Map.of("found", false, "msg", "Nenhuma aposta encontrada hoje")
            );
        }

        return ResponseEntity.ok(Map.of("found", true, "ticket", response.getTicket())
        );
    }

    @PostMapping("/salvar")
    public ResponseEntity<?> salvarBilhete(@RequestBody Map<String, Object> body) {

        Map<String, Object> ticketJson = (Map<String, Object>) body.get("ticket");

        Ticket saved = ticketService.saveAndLink(ticketJson);
        return ResponseEntity.ok(saved);
    }


    @GetMapping("/historico-bilhetes")
    public ResponseEntity<?> historicoBilhetes(
                    @RequestParam(name = "page", defaultValue = "1") int page,
                    @RequestParam(name = "size", defaultValue = "20") int size,
                    @RequestParam(name = "start", required = false) String start,
                    @RequestParam(name = "end", required = false) String end
    ) {

        if (start != null && start.isBlank())
            start = null;
        if (end != null && end.isBlank())
            end = null;

        var result = iaClient.getHistoricoBilhetes(page, size, start, end);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/processar-bilhetes")
    public ResponseEntity<?> processarBilhetes() {
        return ResponseEntity.ok(iaClient.processarBilhetes());
    }

    @GetMapping("/jogos-futuros")
    public ResponseEntity<?> jogosFuturos() {
        var result = iaClient.getJogosFuturos();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/jogos-historicos")
    public ResponseEntity<?> jogosHistoricos(
                    @RequestParam(name = "page", required = false, defaultValue = "1") Integer page,
                    @RequestParam(name = "size", required = false, defaultValue = "20") Integer size,
                    @RequestParam(name = "team", required = false) String team,
                    @RequestParam(name = "league", required = false) String league,
                    @RequestParam(name = "start", required = false) String start,
                    @RequestParam(name = "end", required = false) String end,
                    @RequestParam(name = "sort", required = false, defaultValue = "asc") String sort
    ) {

        return ResponseEntity.ok(
                        iaClient.getJogosHistoricos(page, size, team, league, start, end, sort)
        );
    }


}

