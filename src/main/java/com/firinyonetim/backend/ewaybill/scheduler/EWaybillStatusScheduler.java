package com.firinyonetim.backend.ewaybill.scheduler;

import com.firinyonetim.backend.ewaybill.service.EWaybillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EWaybillStatusScheduler {

    private final EWaybillService eWaybillService;

    public void checkPendingEWaybillStatuses() {
        log.info("Scheduler running: Checking for pending e-waybill statuses...");
        eWaybillService.checkAndUpdateStatuses();
    }
}