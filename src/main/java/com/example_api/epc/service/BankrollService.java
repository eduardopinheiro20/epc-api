package com.example_api.epc.service;

import com.example_api.epc.entity.Ticket;

import java.util.Map;

public interface BankrollService {

    Map<String,Object> getCurrent();

    Map<String,Object> create(double initial);

    Map<String,Object> reset();

    Map<String, Object> vincularTicket(Long ticketId);

    Map<String, Object> validarAutomatico();

    Map<String, Object>  cashout(double valorFinal);
}
