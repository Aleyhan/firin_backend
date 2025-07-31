// src/main/java/com/firinyonetim/backend/controller/ReportController.java
package com.firinyonetim.backend.controller;

import com.firinyonetim.backend.dto.route.DailyRouteLedgerResponseDto;
import com.firinyonetim.backend.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('YONETICI', 'DEVELOPER', 'MUHASEBE')")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/daily-route-ledger")
    public ResponseEntity<DailyRouteLedgerResponseDto> getDailyRouteLedger(
            @RequestParam Long routeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(reportService.getDailyRouteLedger(routeId, date));
    }
}