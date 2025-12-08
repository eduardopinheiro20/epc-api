package com.example_api.epc.client;

import com.example_api.epc.dto.TicketResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
public class IaClient {

    private final WebClient webClient;

    public IaClient() {
        this.webClient = WebClient.builder()
                        .baseUrl("http://localhost:8000")
                        .build();
    }

    // chama o endpoint FASTAPI
    public TicketResponse getBilheteDoDia() {
        return webClient.get()
                        .uri("/bilhete-do-dia")
                        .retrieve()
                        .bodyToMono(TicketResponse.class)
                        .block();
    }

    // CHAMA o Python
    public Map salvarNoPython(Object ticket) {
        return webClient.post()
                        .uri("/salvar-bilhete")
                        .bodyValue(Map.of("ticket", ticket))
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();
    }

    public Map<String, Object> getHistoricoBilhetes(int page, int size, String start, String end) {

        return webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                        .path("/historico-tickets")
                                        .queryParam("page", page)
                                        .queryParam("size", size)
                                        .queryParam("start", start)
                                        .queryParam("end", end)
                                        .build())
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                        .block();
    }

    public Map<String, Object> getJogosFuturos() {
        return webClient.get()
                        .uri("/jogos-futuros")
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                        .block();
    }

    public Map<String, Object> getJogosHistoricos(
                    Integer page,
                    Integer size,
                    String team,
                    String league,
                    String start,
                    String end,
                    String sort
    ) {

        return webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                        .path("/jogos-historicos")
                                        .queryParam("page", page)
                                        .queryParam("size", size)
                                        .queryParam("team", team)
                                        .queryParam("league", league)
                                        .queryParam("start", start)
                                        .queryParam("end", end)
                                        .queryParam("sort", sort)
                                        .build())
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                        .block();
    }

    public Map<String, Object> getBankroll() {
        return webClient.get()
                        .uri("/bankroll")
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Map<String,Object>>() {})
                        .block();
    }

    // POST /bankroll/create
    public Map<String, Object> createBankroll(double initial) {
        return webClient.post()
                        .uri("/bankroll/create")
                        .bodyValue(Map.of("initial", initial))
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Map<String,Object>>() {})
                        .block();
    }

    // POST /bankroll/apply-ticket
    public Map<String, Object> applyTicket(Long ticketId) {
        return webClient.post()
                        .uri("/bankroll/apply-ticket")
                        .bodyValue(Map.of("ticket_id", ticketId))
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Map<String,Object>>() {})
                        .block();
    }

    // POST /bankroll/reset
    public Map<String, Object> resetBankroll() {
        return webClient.post()
                        .uri("/bankroll/reset")
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Map<String,Object>>() {})
                        .block();
    }

    // GET /cron/validar-tickets
    public Map<String, Object> validarTicketsCron() {
        return webClient.get()
                        .uri("/cron/validar-tickets")
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Map<String,Object>>() {})
                        .block();
    }

    // POST /processar-bilhetes
    public Map<String, Object> processarBilhetes() {
        return webClient.post()
                        .uri("/processar-bilhetes")
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Map<String,Object>>() {})
                        .block();
    }

}
