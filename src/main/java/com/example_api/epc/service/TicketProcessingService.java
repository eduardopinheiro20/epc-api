package com.example_api.epc.service;

import java.util.Map;

public interface TicketProcessingService {

    Map<String, Object> processPendingTickets();

    boolean processSingleTicket(Long ticket);

}
