package com.example_api.epc.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class IaTicketSelectionDto {

    @JsonProperty("fixture_id")
    private Long fixtureId;

    private String home;
    private String away;

    private String market;

    @JsonProperty("market_code")
    private String marketCode;

    @JsonProperty("market_group")
    private String marketGroup;

    private Double odd;
    private Double prob;

    private OffsetDateTime date;

    @JsonProperty("odd_source")
    private String oddSource;

    @JsonProperty("odd_captured_at")
    private OffsetDateTime oddCapturedAt;

    @JsonProperty("analysis_quality")
    private Double analysisQuality;
}
