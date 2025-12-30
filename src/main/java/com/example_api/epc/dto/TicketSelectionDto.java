package com.example_api.epc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TicketSelectionDto {

    private Long fixtureId;
    private String market;
    private Double odd;
    private Double prob;
    private String homeName;
    private String awayName;
}


