package com.example_api.epc.service.impl;

import com.example_api.epc.entity.Bankroll;
import com.example_api.epc.entity.Ticket;
import com.example_api.epc.repository.BankrollRepository;
import com.example_api.epc.repository.TicketRepository;
import com.example_api.epc.service.BankrollService;
import com.example_api.epc.service.TicketProcessingService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@AllArgsConstructor
public class BankrollServiceImpl implements BankrollService {

    @Autowired
    private BankrollRepository repository;

    @Autowired
    private TicketRepository ticketRepo;

    @Autowired
    private TicketProcessingService ticketProcessingService;

    @Override
    public Map<String,Object> getCurrent() {
        Optional<Bankroll> active = repository.findActiveBankroll();

        if (active.isEmpty()) {
            return Map.of("exists", false);
        }

        return Map.of(
                        "exists", true,
                        "bankroll", active.get()
        );
    }

    @Override
    @Transactional
    public Map<String,Object>   create(double initial) {
        Optional<Bankroll> existing = repository.findActiveBankroll();

        if (existing.isPresent()) {
            return Map.of("error", "Já existe banca ativa.");
        }

        Bankroll b = new Bankroll();
        b.setInitialAmount(initial);
        b.setCurrentAmount(initial);
        b.setStatus("ACTIVE");

        repository.save(b);

        return Map.of("created", true, "bankroll", b);
    }

    @Override
    @Transactional
    public Map<String,Object> reset() {
        repository.deleteAll();
        return Map.of("reset", true);
    }

    @Override
    @Transactional
    public Map<String, Object> vincularTicket(Long ticketId) {

        Optional<Bankroll> activeOpt = repository.findActiveBankroll();
        if (activeOpt.isEmpty()) {
            return Map.of("error", "Nenhuma banca ativa.");
        }

        Optional<Ticket> tOpt = ticketRepo.findById(ticketId);
        if (tOpt.isEmpty()) {
            return Map.of("error", "Ticket não encontrado.");
        }

        Bankroll b = activeOpt.get();
        Ticket ticket = tOpt.get();

        // Vincula à banca e seta como aplicado futuro
        ticket.setBankroll(b);
        ticket.setAppliedToBankroll(false);
        ticketRepo.save(ticket);

        return Map.of("linked", true);
    }

    @Override
    public Map<String, Object> validarAutomatico() {
        // PROCESSA OS BILHETES PENDENTES
        Map<String, Object> result = ticketProcessingService.processPendingTickets();

        // Retorna a banca atualizada
        Bankroll active = repository.findByStatus("ACTIVE");
        result.put("bankroll", active);

        return result;
    }

}
