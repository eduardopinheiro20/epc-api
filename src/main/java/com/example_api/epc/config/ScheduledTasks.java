package com.example_api.epc.config;

import com.example_api.epc.service.TicketProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScheduledTasks {

    private final TicketProcessingService ticketService;

    @Scheduled(cron = "0 0 23 * * *")
    public void nightlyProcess() {
        ticketService.processPendingTickets();
    }

    @Scheduled(cron = "0 0 7 * * *")
    public void morningProcess() {
        ticketService.processPendingTickets();
    }
}
