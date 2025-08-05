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

    // cron = "saniye dakika saat gün ay gün(hafta)"
    // Bu ayar her dakikanın 0. saniyesinde çalışır. (örn: 10:01:00, 10:02:00)
    @Scheduled(cron = "0 * * * * *")
    public void checkPendingEWaybillStatuses() {
        log.info("Scheduler running: Checking for pending e-waybill statuses...");
        eWaybillService.checkAndUpdateStatuses();
    }
}