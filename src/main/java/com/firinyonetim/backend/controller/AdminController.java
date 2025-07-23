// src/main/java/com/firinyonetim/backend/controller/AdminController.java
package com.firinyonetim.backend.controller;

import com.firinyonetim.backend.dto.transaction.response.TransactionResponse;
import com.firinyonetim.backend.service.RouteService;
import com.firinyonetim.backend.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // Import'u ekleyin
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final TransactionService transactionService;
    private final RouteService routeService;

    @GetMapping("/pending-transactions")
    @PreAuthorize("hasAnyRole('YONETICI', 'DEVELOPER', 'MUHASEBE')") // DEĞİŞİKLİK BURADA
    public ResponseEntity<List<TransactionResponse>> getPendingTransactions() {
        return ResponseEntity.ok(transactionService.getPendingTransactions());
    }

    @PostMapping("/transactions/{transactionId}/approve")
    @PreAuthorize("hasAnyRole('YONETICI', 'DEVELOPER', 'MUHASEBE')") // DEĞİŞİKLİK BURADA
    public ResponseEntity<TransactionResponse> approveTransaction(@PathVariable Long transactionId) {
        return ResponseEntity.ok(transactionService.approveTransaction(transactionId));
    }

    @PostMapping("/transactions/{transactionId}/reject")
    @PreAuthorize("hasAnyRole('YONETICI', 'DEVELOPER', 'MUHASEBE')") // DEĞİŞİKLİK BURADA
    public ResponseEntity<TransactionResponse> rejectTransaction(@PathVariable Long transactionId, @RequestBody Map<String, String> payload) {
        String reason = payload.get("reason");
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Reddetme sebebi boş olamaz.");
        }
        return ResponseEntity.ok(transactionService.rejectTransaction(transactionId, reason));
    }

    @PutMapping("/routes/{routeId}/delivery-order")
    @PreAuthorize("hasAnyRole('YONETICI', 'DEVELOPER')") // Bu endpoint muhasebe için gereksiz, olduğu gibi kalabilir.
    public ResponseEntity<Void> updateDeliveryOrder(@PathVariable Long routeId, @RequestBody List<Long> orderedCustomerIds) {
        routeService.updateDeliveryOrder(routeId, orderedCustomerIds);
        return ResponseEntity.ok().build();
    }
}