// src/main/java/com/firinyonetim/backend/controller/AdminController.java
package com.firinyonetim.backend.controller;

import com.firinyonetim.backend.dto.shipment.response.ShipmentReportResponse; // YENİ
import com.firinyonetim.backend.dto.transaction.response.TransactionResponse;
import com.firinyonetim.backend.service.RouteService;
import com.firinyonetim.backend.service.ShipmentService; // YENİ
import com.firinyonetim.backend.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('YONETICI', 'DEVELOPER', 'MUHASEBE')")
public class AdminController {

    private final TransactionService transactionService;
    private final RouteService routeService;
    private final ShipmentService shipmentService; // YENİ

    @GetMapping("/pending-transactions")
    public ResponseEntity<List<TransactionResponse>> getPendingTransactions() {
        return ResponseEntity.ok(transactionService.getPendingTransactions());
    }

    @PostMapping("/transactions/{transactionId}/approve")
    public ResponseEntity<TransactionResponse> approveTransaction(@PathVariable Long transactionId) {
        return ResponseEntity.ok(transactionService.approveTransaction(transactionId));
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

    // YENİ ENDPOINT'LER
    @GetMapping("/shipments/completed")
    public ResponseEntity<List<ShipmentReportResponse>> getCompletedShipments() {
        return ResponseEntity.ok(shipmentService.getCompletedShipments());
    }

    @GetMapping("/shipments/{id}")
    public ResponseEntity<ShipmentReportResponse> getShipmentById(@PathVariable Long id) {
        return ResponseEntity.ok(shipmentService.getShipmentReportById(id));
    }
}