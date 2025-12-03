package com.example_api.epc.service;

import com.example_api.epc.entity.Ticket;

import java.util.Map;

public interface TicketProcessingService {

    Map<String, Object> processPendingTickets();

    boolean processSingleTicket(Long ticket);

    Ticket saveAndLink(Map<String, Object> pTicketJson);
}
