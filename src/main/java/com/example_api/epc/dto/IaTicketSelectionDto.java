package com.example_api.epc.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class IaTicketSelectionDto {

    @JsonProperty("fixture_id")
    private Long fixtureId;

    private String home;
    private String away;

    private String market;
    private Double odd;
    private Double prob;

    private LocalDateTime date;
}