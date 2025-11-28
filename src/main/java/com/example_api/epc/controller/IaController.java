package com.example_api.epc.controller;

import com.example_api.epc.client.IaClient;
import com.example_api.epc.dto.TicketResponse;
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
        Object ticket = body.get("ticket");

        // resposta real do Python
        Map<String, Object> result = iaClient.salvarNoPython(ticket);

        return ResponseEntity.ok(result);
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
                    @RequestParam(name = "start", required = false) String start,
                    @RequestParam(name = "end", required = false) String end
    ) {
        var result = iaClient.getJogosHistoricos(start, end);
        return ResponseEntity.ok(result);
    }

}

