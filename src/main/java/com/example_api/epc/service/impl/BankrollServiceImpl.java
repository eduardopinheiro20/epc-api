package com.example_api.epc.service.impl;

import com.example_api.epc.dto.BankrollDto;
import com.example_api.epc.entity.Bankroll;
import com.example_api.epc.entity.Ticket;
import com.example_api.epc.entity.User;
import com.example_api.epc.mapper.BankrollMapper;
import com.example_api.epc.repository.BankrollRepository;
import com.example_api.epc.repository.TicketRepository;
import com.example_api.epc.service.AuthenticatedUserService;
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

    @Autowired
    private AuthenticatedUserService authUserService;

    @Override
    public Map<String, Object> getCurrent() {

        User user = authUserService.getCurrentUser();

        Optional<Bankroll> active =
                        repository.findByUserAndStatus(user, "ACTIVE");

        if (active.isEmpty()) {
            return Map.of("exists", false);
        }

        Bankroll b = active.get();

        BankrollDto dto = new BankrollDto(
                        b.getId(),
                        b.getName(),
                        b.getInitialAmount(),
                        b.getCurrentAmount(),
                        b.getStatus(),
                        b.getCreatedAt(),
                        b.getUpdatedAt()
        );

        return Map.of(
                        "exists", true,
                        "bankroll", dto
        );
    }


    @Override
    @Transactional
    public Map<String,Object> create(double initial) {

        User user = authUserService.getCurrentUser();
        Optional<Bankroll> existing =
                        repository.findByUserAndStatus(user, "ACTIVE");

        if (existing.isPresent()) {
            return Map.of("error", "Já existe banca ativa.");
        }

        Bankroll b = new Bankroll();
        b.setInitialAmount(initial);
        b.setCurrentAmount(initial);
        b.setStatus("ACTIVE");
        b.setUser(user);

        Bankroll bankrollSaved = repository.save(b);

        return Map.of("created", true, "bankroll", BankrollMapper.toDto(bankrollSaved));
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
    @Transactional
    public Map<String, Object> validarAutomatico() {

        User user = authUserService.getCurrentUser();
        long count = ticketRepository.countProcessableTickets(user);

        // NADA PARA PROCESSAR
        if (count == 0) {
            return Map.of(
                            "updated", false,
                            "reason", "NO_FINALIZED_GAMES",
                            "message", "Não é possível atualizar a banca. Nenhum jogo finalizado ou dados ainda não coletados."
            );
        }

        Map<String, Object> result =
                        ticketProcessingService.processPendingTickets();

        iaTrainingService.retrain();

        Optional<Bankroll> activeOpt =
                        repository.findByUserAndStatus(user, "ACTIVE");

        if (activeOpt.isPresent()) {
            Bankroll active = activeOpt.get();

            BankrollDto dto = new BankrollDto(
                            active.getId(),
                            active.getName(),
                            active.getInitialAmount(),
                            active.getCurrentAmount(),
                            active.getStatus(),
                            active.getCreatedAt(),
                            active.getUpdatedAt()
            );

            result.put("bankroll", dto);
        } else {
            result.put("bankroll", null);
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
    public Bankroll getActiveBankroll(User pUser) {
        return repository.findByUserAndStatus(pUser,"ACTIVE").orElse(null);
    }

}
