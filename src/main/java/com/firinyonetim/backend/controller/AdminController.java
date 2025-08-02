// src/main/java/com/firinyonetim/backend/controller/AdminController.java
package com.firinyonetim.backend.controller;

import com.firinyonetim.backend.dto.PagedResponseDto;
import com.firinyonetim.backend.dto.shipment.request.ShipmentUpdateRequest;
import com.firinyonetim.backend.dto.shipment.response.ShipmentReportResponse;
import com.firinyonetim.backend.dto.transaction.response.TransactionResponse;
import com.firinyonetim.backend.entity.ShipmentStatus;
import com.firinyonetim.backend.service.RouteService;
import com.firinyonetim.backend.service.ShipmentService;
import com.firinyonetim.backend.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('YONETICI', 'DEVELOPER', 'MUHASEBE')")
public class AdminController {

    private final TransactionService transactionService;
    private final RouteService routeService;
    private final ShipmentService shipmentService;

    @GetMapping("/pending-transactions")
    public ResponseEntity<List<TransactionResponse>> getPendingTransactions() {
        return ResponseEntity.ok(transactionService.getPendingTransactions());
    }

    @PostMapping("/transactions/{transactionId}/approve")
    public ResponseEntity<TransactionResponse> approveTransaction(@PathVariable Long transactionId) {
        return ResponseEntity.ok(transactionService.approveTransaction(transactionId));
    }

    // YENİ ENDPOINT
    @PostMapping("/transactions/approve-batch")
    public ResponseEntity<List<TransactionResponse>> approveMultipleTransactions(@RequestBody List<Long> transactionIds) {
        return ResponseEntity.ok(transactionService.approveMultipleTransactions(transactionIds));
    }

    @PostMapping("/transactions/{transactionId}/reject")
    public ResponseEntity<TransactionResponse> rejectTransaction(@PathVariable Long transactionId, @RequestBody Map<String, String> payload) {
        String reason = payload.get("reason");
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Reddetme/iptal sebebi boş olamaz.");
        }
        return ResponseEntity.ok(transactionService.rejectTransaction(transactionId, reason));
    }

    @PutMapping("/routes/{routeId}/delivery-order")
    public ResponseEntity<Void> updateDeliveryOrder(@PathVariable Long routeId, @RequestBody List<Long> orderedCustomerIds) {
        routeService.updateDeliveryOrder(routeId, orderedCustomerIds);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/shipments")
    public ResponseEntity<PagedResponseDto<ShipmentReportResponse>> searchShipments(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long routeId,
            @RequestParam(required = false) Long driverId,
            @RequestParam(required = false) ShipmentStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(shipmentService.searchShipments(startDate, endDate, routeId, driverId, status, pageable));
    }

    @GetMapping("/shipments/{id}")
    public ResponseEntity<ShipmentReportResponse> getShipmentById(@PathVariable Long id) {
        return ResponseEntity.ok(shipmentService.getShipmentReportById(id));
    }

    @PutMapping("/shipments/{id}")
    public ResponseEntity<ShipmentReportResponse> updateShipment(
            @PathVariable Long id,
            @Valid @RequestBody ShipmentUpdateRequest request) {
        return ResponseEntity.ok(shipmentService.updateShipment(id, request));
    }

}