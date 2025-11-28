package com.example_api.epc.controller;

import com.example_api.epc.service.BankrollService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/bankroll")
@CrossOrigin(origins = "*")
public class BankrollController {

    @Autowired
    private BankrollService service;

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

    @PostMapping("/apply-ticket")
    public ResponseEntity<?> applyTicket(@RequestBody Map<String,Object> req) {
        Long ticketId = Long.parseLong(req.get("ticket_id").toString());
        return ResponseEntity.ok(service.applyTicket(ticketId));
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

}