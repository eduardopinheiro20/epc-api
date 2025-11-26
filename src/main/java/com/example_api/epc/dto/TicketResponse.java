package com.example_api.epc.dto;

import lombok.Data;

import java.util.Map;

@Data
public class TicketResponse {

    private boolean found;
    private Map<String, Object> ticket;
}
