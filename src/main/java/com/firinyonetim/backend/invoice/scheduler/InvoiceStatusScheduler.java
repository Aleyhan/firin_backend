package com.firinyonetim.backend.invoice.scheduler;

import com.firinyonetim.backend.invoice.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceStatusScheduler {

    private final InvoiceService invoiceService;

    public void checkPendingInvoiceStatuses() {
        log.info("Scheduler running: Checking for pending invoice statuses...");
        invoiceService.checkAndUpdateStatuses();
    }
}