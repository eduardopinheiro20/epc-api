package com.example_api.epc.service.impl;

import com.example_api.epc.dto.TicketSavedDto;
import com.example_api.epc.dto.TicketSelectionDto;
import com.example_api.epc.entity.*;
import com.example_api.epc.repository.BankrollRepository;
import com.example_api.epc.repository.FixtureRepository;
import com.example_api.epc.repository.TicketRepository;
import com.example_api.epc.repository.TicketSelectionRepository;
import com.example_api.epc.service.AuthenticatedUserService;
import com.example_api.epc.service.TicketProcessingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketProcessingServiceImpl implements TicketProcessingService {

    private final AuthenticatedUserService authUserService;
    private final TicketRepository ticketRepository;
    private final TicketSelectionRepository selectionRepository;
    private final FixtureRepository fixtureRepository;
    private final BankrollRepository bankrollRepository;
    private final TicketSelectionRepository ticketSelectionRepository;

    @Override
    @Transactional
    public Map<String, Object> saveAndLink(Map<String, Object> json) {

        User user = authUserService.getCurrentUser();

        try {
            // ------------------------------
            // Get Banca Ativa
            // ------------------------------
            Optional<Bankroll> opt = bankrollRepository.findByUserAndStatus(user, "ACTIVE");

            // ------------------------------
            // 1) MONTAR O TICKET
            // ------------------------------
            Ticket t = new Ticket();
            t.setCreatedBy("SYSTEM");
            t.setSavedAt(LocalDateTime.now());
            t.setStatus("PENDING");
            t.setResult(null);
            t.setUser(user);

            if (opt.isPresent()) {
                Bankroll bankroll = opt.get();
                t.setAppliedToBankroll(true);
                t.setBankroll(bankroll);
            } else {
                t.setAppliedToBankroll(false);
                t.setBankroll(null);
            }

            // segurança: validar nulls
            Object finalOddObj = json.get("final_odd");
            Object combinedProbObj = json.get("combined_prob");

            double finalOdd = finalOddObj != null ? Double.parseDouble(finalOddObj.toString()) : 1.0;
            double combinedProb = combinedProbObj != null ? Double.parseDouble(combinedProbObj.toString()) : 0.0;

            t.setFinalOdd(finalOdd);
            t.setCombinedProb(combinedProb);

            // META (guarda campos opcionais)
            Map<String, Object> meta = new HashMap<>();
            meta.put("explanation", json.get("explanation"));
            meta.put("system_recommendation", json.get("system_recommendation"));
            meta.put("confidence_formatted", json.get("confidence_formatted"));
            t.setMeta(meta.toString());

            // ------------------------------
            // SELEÇÕES (para assinatura)
            // ------------------------------
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> selections = (List<Map<String, Object>>) json.getOrDefault("selections", Collections.emptyList());

            // SERIALIZAR somente a identidade estável das seleções.
            // Explicação e horário de captura da odd não podem criar duplicata.
            List<Map<String, Object>> signatureSelections = selections.stream()
                            .map(selection -> {
                                Map<String, Object> item = new HashMap<>();
                                item.put("fixture_id", selection.get("fixture_id"));
                                item.put(
                                                "market_code",
                                                selection.get("market_code") != null
                                                                ? selection.get("market_code")
                                                                : selection.get("market")
                                );
                                return item;
                            })
                            .sorted(Comparator.comparing(item ->
                                            Objects.toString(item.get("fixture_id"), "")
                                                            + "|"
                                                            + Objects.toString(item.get("market_code"), "")
                            ))
                            .toList();

            String signatureRaw;
            ObjectMapper mapper = new ObjectMapper();
            try {
                signatureRaw = mapper.writeValueAsString(signatureSelections);
            } catch (JsonProcessingException e) {
                // fallback simples se Jackson falhar por algum motivo
                signatureRaw = signatureSelections.toString();
            }

            // MD5 com java.security (sem libs externas)
            String signature;
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] digest = md.digest(signatureRaw.getBytes(StandardCharsets.UTF_8));
                signature = String.format("%032x", new BigInteger(1, digest));
            } catch (Exception ex) {
                // fallback: usar hashCode (menos ideal, mas evita crash)
                signature = Integer.toHexString(signatureRaw.hashCode());
            }
            t.setSignature(signature);

            // Verificar duplicata (assuma que exista findBySignature)
            Ticket dupe = ticketRepository.findBySignature(signature);
            if (dupe != null) {
                Map<String, Object> resp = new HashMap<>();
                resp.put("alreadyExists", true);
                resp.put("ticket", dupe);
                return resp;
            }

            // ------------------------------
            // SALVAR TICKET (gera ID)
            // ------------------------------
            ticketRepository.save(t);

            // ------------------------------
            // SALVAR SELEÇÕES
            // ------------------------------
            for (Map<String, Object> s : selections) {
                TicketSelection ts = new TicketSelection();
                ts.setTicketId(t.getId());
                Object fixIdObj = s.get("fixture_id");
                if (fixIdObj != null) {
                    ts.setFixtureId(Long.valueOf(fixIdObj.toString()));
                }
                ts.setHomeName(Objects.toString(s.get("home"), null));
                ts.setAwayName(Objects.toString(s.get("away"), null));
                ts.setMarket(Objects.toString(s.get("market"), null));
                ts.setMarketCode(Objects.toString(s.get("market_code"), null));
                Object oddObj = s.get("odd");
                if (oddObj != null) ts.setOdd(Double.parseDouble(oddObj.toString()));
                ts.setOddSource(Objects.toString(s.get("odd_source"), null));
                Object capturedAtObj = s.get("odd_captured_at");
                if (capturedAtObj != null) {
                    ts.setOddCapturedAt(OffsetDateTime.parse(capturedAtObj.toString()));
                }
                Object probObj = s.get("prob");
                if (probObj != null) ts.setProb(Double.parseDouble(probObj.toString()));

                selectionRepository.save(ts);
            }

            // após salvar seleções
            List<TicketSelectionDto> selectionDtos = selections.stream()
                            .map(s -> new TicketSelectionDto(
                                            s.get("fixture_id") != null ? Long.valueOf(s.get("fixture_id").toString()) : null,
                                            Objects.toString(s.get("market"), null),
                                            s.get("odd") != null ? Double.parseDouble(s.get("odd").toString()) : null,
                                            s.get("prob") != null ? Double.parseDouble(s.get("prob").toString()) : null,
                                            Objects.toString(s.get("home"), null),
                                            Objects.toString(s.get("away"), null)
                            ))
                            .toList();

            TicketSavedDto dto = new TicketSavedDto(
                            t.getId(),
                            t.getStatus(),
                            t.getFinalOdd(),
                            t.getCombinedProb(),
                            t.getAppliedToBankroll(),
                            selectionDtos
            );

            Map<String, Object> resp = new HashMap<>();
            resp.put("alreadyExists", false);
            resp.put("ticket", dto);
            return resp;

        } catch (Exception e) {
            // logue o erro (use seu logger)
            e.printStackTrace();
            throw new RuntimeException("Erro ao salvar e vincular ticket: " + e.getMessage(), e);
        }
    }

    public Page<Ticket> getHistoricoPorBankroll(
                    Long bankrollId,
                    int page,
                    int size,
                    String start,
                    String end
    ) {

        Pageable pageable = PageRequest.of(
                        Math.max(page - 1, 0),
                        size,
                        Sort.by("savedAt").descending()
        );

        LocalDateTime startDt = null;
        LocalDateTime endDt = null;

        if (start != null && !start.isBlank()) {
            startDt = LocalDate.parse(start).atStartOfDay();
        }
        if (end != null && !end.isBlank()) {
            endDt = LocalDate.parse(end).atTime(23, 59, 59);
        }

        Page<Ticket> pageTickets;

        if (startDt == null && endDt == null) {
            pageTickets = ticketRepository.findByBankrollId(bankrollId, pageable);
        } else if (startDt != null && endDt == null) {
            pageTickets = ticketRepository.findByBankrollIdAndSavedAtGreaterThanEqual(
                            bankrollId, startDt, pageable
            );
        } else if (startDt == null) {
            pageTickets = ticketRepository.findByBankrollIdAndSavedAtLessThanEqual(
                            bankrollId, endDt, pageable
            );
        } else {
            pageTickets = ticketRepository.findByBankrollIdAndSavedAtBetween(
                            bankrollId, startDt, endDt, pageable
            );
        }

        // 🔥 AQUI está o pulo do gato
        List<Long> ticketIds = pageTickets
                        .getContent()
                        .stream()
                        .map(Ticket::getId)
                        .toList();

        if (!ticketIds.isEmpty()) {
            List<TicketSelection> selections =
                            ticketSelectionRepository.findByTicketIdIn(ticketIds);

            Map<Long, List<TicketSelection>> map =
                            selections.stream()
                                            .collect(Collectors.groupingBy(TicketSelection::getTicketId));

            pageTickets.getContent().forEach(t ->
                            t.setSelections(map.getOrDefault(t.getId(), List.of()))
            );
        }

        return pageTickets;
    }

    @Override
    @Transactional
    public Map<String, Object> processPendingTickets() {

        Map<String, Object> result = new HashMap<>();

        List<Ticket> pendentes = ticketRepository.findByStatus("PENDING");

        if (pendentes.isEmpty()) {
            result.put("noPendingTickets", true);
            return result;
        }

        int total = pendentes.size();
        int processados = 0;
        int ignorados = 0;

        for (Ticket t : pendentes) {

            // Seleções do bilhete
            List<TicketSelection> sels = selectionRepository.findByTicketId(t.getId());
            boolean allFinished = true;

            // Verifica se TODOS os fixtures já terminaram
            for (TicketSelection sel : sels) {
                Fixture f = fixtureRepository.findById(sel.getFixtureId()).orElse(null);

                if (f == null || !isFixtureFinished(f.getStatus())) {
                    allFinished = false;
                    break;
                }
            }

            // ❌ Ainda não pode processar
            if (!allFinished) {
                ignorados++;
                continue;
            }

            // ✔ Pode processar o bilhete
            processSingleTicket(t.getId());
            processados++;
        }

        result.put("totalPendentes", total);
        result.put("processados", processados);
        result.put("ignorados", ignorados);

        return result;
    }

    @Override
    @Transactional
    public boolean processSingleTicket(Long ticketId) {

        Ticket ticket = ticketRepository.findById(ticketId).orElse(null);
        if (ticket == null) return false;

        // Já processado
        if ("FINISHED".equals(ticket.getStatus()))
            return false;

        List<TicketSelection> selections = selectionRepository.findByTicketId(ticketId);

        boolean allGreen = true;

        for (TicketSelection sel : selections) {

            Fixture f = fixtureRepository.findById(sel.getFixtureId()).orElse(null);
            if (f == null) return false;

            boolean win = evaluateSelection(sel, f);

            if (!win) {
                allGreen = false;
                break;
            }
        }

        ticket.setStatus("FINISHED");
        ticket.setResult(allGreen ? "GREEN" : "RED");
        ticketRepository.save(ticket);

        applyBankrollIfNeeded(ticket);

        return true;
    }

    private boolean isFixtureFinished(String status) {
        if (status == null) return false;
        status = status.toUpperCase();
        return status.startsWith("FT") || status.equals("AET") || status.equals("P");
    }

    private boolean evaluateSelection(TicketSelection sel, Fixture f) {

        int home = Optional.ofNullable(f.getHomeGoals()).orElse(0);
        int away = Optional.ofNullable(f.getAwayGoals()).orElse(0);
        int total = home + away;
        String homeTeam = f.getHomeTeam().getName().toLowerCase();
        String awayTeam = f.getAwayTeam().getName().toLowerCase();
        String market = Optional.ofNullable(sel.getMarket()).orElse("").toLowerCase();
        String marketCode = Optional.ofNullable(sel.getMarketCode()).orElse("").toUpperCase();

        try {
            switch (marketCode) {
                case "TOTAL_OVER_0_5":
                    return total > 0;
                case "TOTAL_UNDER_5_5":
                    return total <= 5;
                case "TOTAL_UNDER_6_5":
                    return total <= 6;
                case "HOME_TEAM_UNDER_2_5":
                    return home <= 2;
                case "AWAY_TEAM_UNDER_2_5":
                    return away <= 2;
                case "HOME_HANDICAP_PLUS_3":
                    return home + 3 > away;
                case "AWAY_HANDICAP_PLUS_3":
                    return away + 3 > home;
                case "HOME_HANDICAP_PLUS_2":
                    return home + 2 > away;
                case "AWAY_HANDICAP_PLUS_2":
                    return away + 2 > home;
                default:
                    // Compatibilidade com bilhetes salvos antes dos códigos.
            }

            if (market.startsWith("under")) {
                double limit = Double.parseDouble(market.replace("under", "").trim());
                return total <= limit;
            }

            if (market.startsWith("over")) {
                double limit = Double.parseDouble(market.replace("over", "").trim());
                return total > limit;
            }

            if (market.equals(awayTeam + " under 2.5")) {
                return away <= 2.5;
            }

            if (market.equals(awayTeam + " under 3.5")) {
                return away <= 3.5;
            }

            if (market.contains("handicap +")) {

                // handicap tipo "+3 (away)"
                String nStr = market.split("\\+")[1].split(" ")[0];
                int n = Integer.parseInt(nStr);

                // Handicap europeu de três vias: empate após aplicar o
                // handicap não vence a seleção do time.
                boolean selectedAway = market.contains(awayTeam)
                                || market.contains("(away)");
                return selectedAway ? (away + n) > home : (home + n) > away;
            }

            if (market.matches(".*\\+[23]$")) {
                int n = Integer.parseInt(market.substring(market.length() - 1));
                if (market.startsWith(awayTeam)) {
                    return away + n > home;
                }
                if (market.startsWith(homeTeam)) {
                    return home + n > away;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return false;
    }

    private void applyBankrollIfNeeded(Ticket ticket) {

        if (!Boolean.TRUE.equals(ticket.getAppliedToBankroll())) return;
        if (ticket.getBankroll() == null) return;

        Bankroll b = bankrollRepository.findById(ticket.getBankroll().getId()).orElse(null);
        if (b == null) return;

        if ("GREEN".equals(ticket.getResult())) {

            // Multiplica a banca
            double newAmount = b.getCurrentAmount() * ticket.getFinalOdd();
            b.setCurrentAmount(newAmount);

            // IMPORTANTE:
            // A banca CONTINUA ativa para a próxima rodada
            b.setStatus("ACTIVE");

        } else {

            // RED → banca é encerrada
            b.setCurrentAmount(0.0);
            b.setStatus("FINISHED");  // ENCERRA SOMENTE QUANDO PERDE
        }

        bankrollRepository.save(b);
    }

    @Override
    public Page<Ticket> getHistoricoCompletoPorUsuario(Long userId,
                    int page,
                    int size,
                    String start,
                    String end
    ) {
        Pageable pageable = PageRequest.of(
                        page - 1, size, Sort.by("savedAt").descending()
        );

        LocalDateTime startDt = (start != null && !start.isBlank())
                        ? LocalDate.parse(start).atStartOfDay()
                        : null;

        LocalDateTime endDt = (end != null && !end.isBlank())
                        ? LocalDate.parse(end).atTime(23, 59, 59)
                        : null;

        return ticketRepository.findByUserIdWithFilters(userId,
                        startDt, endDt, pageable
        );
    }

}
