package com.example_api.epc.service.impl;

import com.example_api.epc.dto.BankrollDto;
import com.example_api.epc.entity.Bankroll;
import com.example_api.epc.entity.Ticket;
import com.example_api.epc.repository.BankrollRepository;
import com.example_api.epc.repository.TicketRepository;
import com.example_api.epc.service.BankrollService;
import com.example_api.epc.service.IaTrainingService;
import com.example_api.epc.service.TicketProcessingService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@AllArgsConstructor
public class BankrollServiceImpl implements BankrollService {

    @Autowired
    private BankrollRepository repository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TicketProcessingService ticketProcessingService;

    @Autowired
    private IaTrainingService iaTrainingService;

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

        Optional<Ticket> tOpt = ticketRepository.findById(ticketId);
        if (tOpt.isEmpty()) {
            return Map.of("error", "Ticket não encontrado.");
        }

        Bankroll b = activeOpt.get();
        Ticket ticket = tOpt.get();

        // Vincula à banca e seta como aplicado futuro
        ticket.setBankroll(b);
        ticket.setAppliedToBankroll(false);
        ticketRepository.save(ticket);

        return Map.of("linked", true);
    }

    @Override
    public Map<String, Object> validarAutomatico() {
        // PROCESSA OS BILHETES PENDENTES
        Map<String, Object> result = ticketProcessingService.processPendingTickets();

        iaTrainingService.retrain();

        // Retorna a banca atualizada
        Bankroll active = repository.findByStatus("ACTIVE");

        result.put("bankroll", active);    if (active != null) {
            BankrollDto dto = new BankrollDto(
                    active.getName(),
                    active.getInitialAmount(),
                    active.getCurrentAmount(),
                    active.getStatus(),
                    active.getCreatedAt(),
                    active.getUpdatedAt()
            );

            result.put("bankroll", dto);
        }

        return result;
    }

    @Transactional
    @Override
    public Map<String, Object> cashout(double valorFinal) {
        Map<String, Object> resp = new HashMap<>();

        Ticket t = ticketRepository.findFirstByStatusOrderBySavedAtDesc("PENDING");

        if (t == null) {
            resp.put("success", false);
            resp.put("message", "Nenhum bilhete pendente para cashout.");
            return resp;
        }

        Bankroll b = repository.findByStatus("ACTIVE");

        b.setCurrentAmount(valorFinal);
        repository.save(b);

        t.setStatus("FINISHED");
        t.setResult("CASHOUT");

        Map<String, Object> meta = new HashMap<>();
        meta.put("cashout_value", valorFinal);
        meta.put("cashout_at", LocalDateTime.now().toString());
        t.setMeta(meta.toString());

        ticketRepository.save(t);

        resp.put("success", true);
        resp.put("message", "Cashout realizado com sucesso.");
        resp.put("newBalance", b.getCurrentAmount());

        return resp;
    }

    @Override
    public Bankroll getActiveBankroll() {
        return repository.findFirstByStatus("ACTIVE").orElse(null);
    }

}
