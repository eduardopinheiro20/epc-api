package com.example_api.epc.service.impl;

import com.example_api.epc.entity.Bankroll;
import com.example_api.epc.entity.Fixture;
import com.example_api.epc.entity.Ticket;
import com.example_api.epc.entity.TicketSelection;
import com.example_api.epc.repository.BankrollRepository;
import com.example_api.epc.repository.FixtureRepository;
import com.example_api.epc.repository.TicketRepository;
import com.example_api.epc.repository.TicketSelectionRepository;
import com.example_api.epc.service.TicketProcessingService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TicketProcessingServiceImpl implements TicketProcessingService {

    private final TicketRepository ticketRepository;
    private final TicketSelectionRepository selectionRepository;
    private final FixtureRepository fixtureRepository;
    private final BankrollRepository bankrollRepository;

    @Override
    @Transactional
    public Map<String, Object> processPendingTickets() {
        List<Ticket> pending = ticketRepository.findByStatus("PENDING");

        int processed = 0;
        int green = 0;
        int red = 0;

        for (Ticket t : pending) {
            boolean done = processSingleTicket(t.getId());
            if (done) {
                processed++;
                if ("GREEN".equals(t.getResult())) green++;
                if ("RED".equals(t.getResult())) red++;
            }
        }

        return Map.of(
                        "processed", processed,
                        "green", green,
                        "red", red
        );
    }

    @Override
    @Transactional
    public boolean processSingleTicket(Long ticketId) {

        Ticket ticket = ticketRepository.findById(ticketId).orElse(null);
        if (ticket == null) return false;

        List<TicketSelection> selections = selectionRepository.findByTicketId(ticketId);

        // Buscar fixtures
        for (TicketSelection sel : selections) {

            Fixture f = fixtureRepository.findById(sel.getFixtureId()).orElse(null);

            if (f == null) return false;

            if (!isFixtureFinished(f.getStatus())) {
                return false; // ainda não terminou
            }

            boolean win = evaluateSelection(sel, f);
            if (!win) {
                ticket.setStatus("FINISHED");
                ticket.setResult("RED");
                ticketRepository.save(ticket);
                applyBankrollIfNeeded(ticket);
                return true;
            }
        }

        // se todas ganharam
        ticket.setStatus("FINISHED");
        ticket.setResult("GREEN");
        ticketRepository.save(ticket);

        applyBankrollIfNeeded(ticket);

        return true;
    }

    private boolean isFixtureFinished(String status) {
        if (status == null) return false;
        status = status.toUpperCase();
        return status.startsWith("FT") || status.equals("AET") || status.equals("PEN");
    }

    private boolean evaluateSelection(TicketSelection sel, Fixture f) {
        int home = Optional.ofNullable(f.getHomeGoals()).orElse(0);
        int away = Optional.ofNullable(f.getAwayGoals()).orElse(0);
        int total = home + away;

        String market = sel.getMarket().toLowerCase();

        try {
            if (market.startsWith("under")) {
                double limit = Double.parseDouble(market.replace("under", "").trim());
                return total <= Math.floor(limit);
            }

            if (market.startsWith("over")) {
                double limit = Double.parseDouble(market.replace("over", "").trim());
                return total > limit;
            }

            if (market.contains("away under")) {
                double limit = Double.parseDouble(market.replace("away under", "").trim());
                return away <= Math.floor(limit);
            }

            if (market.contains("handicap +")) {
                int n = Integer.parseInt(market.split("\\+")[1].split(" ")[0]);
                return away + n >= home;
            }

        } catch (Exception e) {
            return false;
        }

        return false;
    }

    private void applyBankrollIfNeeded(Ticket ticket) {
        if (!Boolean.TRUE.equals(ticket.getAppliedToBankroll())) return;
        if (ticket.getBankrollId() == null) return;

        Bankroll b = bankrollRepository.findById(ticket.getBankrollId()).orElse(null);
        if (b == null) return;

        if ("GREEN".equals(ticket.getResult())) {
            double mult = b.getCurrentAmount() * ticket.getFinalOdd();
            b.setCurrentAmount(mult);
        } else {
            b.setCurrentAmount(0.0);
        }

        b.setStatus("FINISHED");
        bankrollRepository.save(b);
    }
}

