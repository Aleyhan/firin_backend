// src/main/java/com/firinyonetim/backend/controller/DashboardController.java
package com.firinyonetim.backend.controller;

import com.firinyonetim.backend.dto.dashboard.DailyShipmentSummaryDto; // YENİ
import com.firinyonetim.backend.dto.dashboard.DashboardDailySummaryDto;
import com.firinyonetim.backend.dto.dashboard.DashboardStatsDto;
import com.firinyonetim.backend.dto.transaction.response.TransactionResponse;
import com.firinyonetim.backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('YONETICI', 'DEVELOPER', 'MUHASEBE')") // Rolleri güncelleyelim
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDto> getStats() {
        return ResponseEntity.ok(dashboardService.getDashboardStats());
    }

    @GetMapping("/daily-summary")
    public ResponseEntity<DashboardDailySummaryDto> getDailySummary(
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate queryDate = (date == null) ? LocalDate.now() : date;
        return ResponseEntity.ok(dashboardService.getDailySummary(queryDate));
    }

    @GetMapping("/recent-transactions")
    public ResponseEntity<List<TransactionResponse>> getRecentTransactions() {
        return ResponseEntity.ok(dashboardService.getRecentTransactions());
    }

    // YENİ ENDPOINT
    @GetMapping("/daily-shipment-summary")
    public ResponseEntity<DailyShipmentSummaryDto> getDailyShipmentSummary(
            @RequestParam(value = "date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(dashboardService.getDailyShipmentSummary(date));
    }
}