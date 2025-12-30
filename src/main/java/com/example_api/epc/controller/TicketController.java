package com.example_api.epc.controller;

import com.example_api.epc.entity.Bankroll;
import com.example_api.epc.entity.User;
import com.example_api.epc.mapper.TicketMapper;
import com.example_api.epc.service.AuthenticatedUserService;
import com.example_api.epc.service.BankrollService;
import com.example_api.epc.service.TicketProcessingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ticket")
@CrossOrigin(origins = "http://localhost:4200")
public class TicketController {

    private final TicketProcessingService ticketService;

    private final BankrollService bankrollService;

    private final AuthenticatedUserService authUserService;

    private TicketMapper ticketMapper;

    public TicketController(TicketProcessingService pTicketService, BankrollService pBankrollService,
                    final AuthenticatedUserService pAuthUserService) {
        ticketService = pTicketService;
        bankrollService = pBankrollService;
        authUserService = pAuthUserService;
    }

    @PostMapping("/salvar")
    public ResponseEntity<?> salvarBilhete(@RequestBody Map<String, Object> body) {
        Map<String, Object> ticketJson = (Map<String, Object>) body.get("ticket");
        Map<String, Object> result = ticketService.saveAndLink(ticketJson);
        return ResponseEntity.ok(result);
    }


    @GetMapping("/historico-bilhetes")
    public ResponseEntity<?> historicoBilhetes(
                    @RequestParam(name = "page", defaultValue = "1") int page,
                    @RequestParam(name = "size", defaultValue = "20") int size,
                    @RequestParam(name = "start", required = false) String start,
                    @RequestParam(name = "end", required = false) String end,
                    @RequestParam(name = "all", defaultValue = "false") boolean all
    ) {

        User user = authUserService.getCurrentUser();

        if (all) {
            var pageResult = ticketService.getHistoricoCompletoPorUsuario(user.getId(), page, size, start, end);

            var items = pageResult.getContent()
                            .stream()
                            .map(TicketMapper::toHistoricoDto)
                            .toList();

            return ResponseEntity.ok(
                            Map.of(
                                "items", items,
                                "page", page,
                                "pages", pageResult.getTotalPages()
                            )
            );
        }

        Bankroll active = bankrollService.getActiveBankroll(user);

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

        var items = pageResult.getContent()
                        .stream()
                        .map(TicketMapper::toHistoricoDto)
                        .toList();

        return ResponseEntity.ok(Map.of("items", items, "page", page, "pages", pageResult.getTotalPages())
        );
    }
}
