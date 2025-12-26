package com.example_api.epc.controller;

import com.example_api.epc.entity.Bankroll;
import com.example_api.epc.entity.Ticket;
import com.example_api.epc.service.BankrollService;
import com.example_api.epc.service.TicketProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bankroll")
@CrossOrigin(origins = "*")
public class BankrollController {

    @Autowired
    private BankrollService service;

    @Autowired
    private TicketProcessingService ticketService;

    @Autowired
    private BankrollService bankrollService;

    @GetMapping
    public ResponseEntity<?> getCurrent() {
        return ResponseEntity.ok(service.getCurrent());
    }

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        double initial = Double.parseDouble(body.get("initial").toString());
        return ResponseEntity.ok(service.create(initial));
    }

    @PostMapping("/reset")
    public ResponseEntity<?> reset() {
        return ResponseEntity.ok(service.reset());
    }

    @PostMapping("/link-ticket")
    public ResponseEntity<?> linkTicket(@RequestBody Map<String, Object> data) {
        Long ticketId = Long.parseLong(data.get("ticket_id").toString());
        return ResponseEntity.ok(service.vincularTicket(ticketId));
    }

    @PostMapping("/validar")
    public ResponseEntity<?> validar() {
        Map<String, Object> result = service.validarAutomatico();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/cashout")
    public ResponseEntity<?> cashout(@RequestBody Map<String, Object> body) {
        double valor = Double.parseDouble(body.get("finalValue").toString());
        return ResponseEntity.ok(service.cashout(valor));
    }


    @GetMapping("/historico-bilhetes")
    public ResponseEntity<?> historicoBilhetes(
                    @RequestParam(name = "page", defaultValue = "1") int page,
                    @RequestParam(name = "size", defaultValue = "20") int size,
                    @RequestParam(name = "start", required = false) String start,
                    @RequestParam(name = "end", required = false) String end,
                    @RequestParam(name = "all", defaultValue = "false") boolean all
    ) {

        if (all) {
            var result = ticketService.getHistoricoCompleto(
                            page, size, start, end
            );

            return ResponseEntity.ok(
                            Map.of(
                                    "items", result.getContent(),
                                    "page", page,
                                    "pages", result.getTotalPages()
                            )
            );
        }

        Bankroll active = bankrollService.getActiveBankroll();

        if (active == null) {
            return ResponseEntity.ok(
                            Map.of(
                                            "items", List.of(),
                                            "page", page,
                                            "pages", 0,
                                            "msg", "Nenhuma banca ativa"
                            )
            );
        }

        var pageResult = ticketService.getHistoricoPorBankroll(
                        active.getId(),
                        page,
                        size,
                        start,
                        end
        );

        return ResponseEntity.ok(
                        Map.of(
                            "items", pageResult.getContent(),
                            "page", page,
                            "pages", pageResult.getTotalPages()
                        )
        );

    }

}