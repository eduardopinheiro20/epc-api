package com.example_api.epc.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class IaTicketDto {

    @JsonProperty("target_odd")
    private Double targetOdd;

    @JsonProperty("final_odd")
    private Double finalOdd;

    @JsonProperty("combined_prob")
    private Double combinedProb;

    @JsonProperty("confidence_formatted")
    private String confidenceFormatted;

    @JsonProperty("risk_score")
    private Double riskScore;

    @JsonProperty("system_recommendation")
    private String systemRecommendation;

    private Integer mode;

    private List<IaTicketSelectionDto> selections;
}

