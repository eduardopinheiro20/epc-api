package com.example_api.epc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TicketSavedDto {

    private Long id;
    private String status;
    private Double finalOdd;
    private Double combinedProb;
    private boolean appliedToBankroll;

    private List<TicketSelectionDto> selections;
}

