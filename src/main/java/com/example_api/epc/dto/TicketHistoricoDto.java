package com.example_api.epc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TicketHistoricoDto {

    private Long id;
    private String status;
    private String result;
    private Double finalOdd;
    private Double combinedProb;
    private LocalDateTime savedAt;

    private List<TicketSelectionDto> selections;
}
