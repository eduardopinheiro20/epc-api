package com.example_api.epc.mapper;

import com.example_api.epc.dto.TicketHistoricoDto;
import com.example_api.epc.dto.TicketSelectionDto;
import com.example_api.epc.entity.Ticket;

import java.util.List;

public class TicketMapper {

    public static TicketHistoricoDto toHistoricoDto(Ticket ticket) {

        List<TicketSelectionDto> selections =
                        ticket.getSelections().stream()
                                        .map(sel -> new TicketSelectionDto(
                                                        sel.getFixtureId(),
                                                        sel.getMarket(),
                                                        sel.getOdd(),
                                                        sel.getProb(),
                                                        sel.getHomeName(),
                                                        sel.getAwayName()
                                        ))
                                        .toList();

        return new TicketHistoricoDto(
                        ticket.getId(),
                        ticket.getStatus(),
                        ticket.getResult(),
                        ticket.getFinalOdd(),
                        ticket.getCombinedProb(),
                        ticket.getSavedAt(),
                        selections
        );
    }
}
