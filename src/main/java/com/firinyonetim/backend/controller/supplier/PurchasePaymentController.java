// src/main/java/com/firinyonetim/backend/controller/supplier/PurchasePaymentController.java
package com.firinyonetim.backend.controller.supplier;

import com.firinyonetim.backend.dto.supplier.request.PurchasePaymentRequest;
import com.firinyonetim.backend.dto.supplier.response.PurchasePaymentResponse;
import com.firinyonetim.backend.service.supplier.PurchaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchase-payments")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('YONETICI', 'DEVELOPER', 'MUHASEBE')")
public class PurchasePaymentController {

    private final PurchaseService purchaseService;

    @PostMapping
    public ResponseEntity<PurchasePaymentResponse> createPayment(@Valid @RequestBody PurchasePaymentRequest request) {
        return new ResponseEntity<>(purchaseService.createPayment(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<PurchasePaymentResponse>> getAllPayments() {
        return ResponseEntity.ok(purchaseService.getAllPayments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchasePaymentResponse> getPaymentById(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseService.getPaymentById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PurchasePaymentResponse> updatePayment(@PathVariable Long id, @Valid @RequestBody PurchasePaymentRequest request) {
        return ResponseEntity.ok(purchaseService.updatePurchasePayment(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePayment(@PathVariable Long id) {
        purchaseService.deletePurchasePayment(id);
        return ResponseEntity.noContent().build();
    }
}