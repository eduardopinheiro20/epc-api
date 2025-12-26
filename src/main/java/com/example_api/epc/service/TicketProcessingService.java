package com.example_api.epc.service;

import com.example_api.epc.entity.Ticket;
import org.springframework.data.domain.Page;

import java.util.Map;

public interface TicketProcessingService {

    Map<String, Object> processPendingTickets();

    boolean processSingleTicket(Long ticket);

    Map<String, Object> saveAndLink(Map<String, Object> pTicketJson);

    Page<Ticket> getHistoricoPorBankroll(Long pId, int pPage, int pSize, String pStart, String pEnd);

    Page<Ticket>  getHistoricoCompleto(int pPage, int pSize, String pStart, String pEnd);
}
