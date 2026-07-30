package com.example_api.epc.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TicketResponse {

    private Boolean found;
    private String reason;
    private String message;
    private IaTicketDto ticket;

    @com.fasterxml.jackson.annotation.JsonProperty("odds_required")
    private List<Map<String, Object>> oddsRequired;

    private Map<String, Object> diagnostics;
}
