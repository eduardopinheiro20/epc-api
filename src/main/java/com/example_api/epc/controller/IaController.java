package com.example_api.epc.controller;

import com.example_api.epc.client.IaClient;
import com.example_api.epc.dto.TicketResponse;
import com.example_api.epc.service.BankrollService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ia")
@CrossOrigin(origins = "http://localhost:4200")
public class IaController {

    @Autowired
    private IaClient iaClient;

    private final BankrollService bankrollService;

    public IaController(final BankrollService pBankrollService) {
        bankrollService = pBankrollService;
    }

    @GetMapping("/bilhete-do-dia")
    public ResponseEntity<?> melhorAposta() {

        TicketResponse response = iaClient.getBilheteDoDia();
        return ResponseEntity.ok(response);
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

