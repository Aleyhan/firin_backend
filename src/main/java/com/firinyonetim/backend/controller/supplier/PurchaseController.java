package com.firinyonetim.backend.controller.supplier;

import com.firinyonetim.backend.dto.supplier.request.PurchasePaymentRequest;
import com.firinyonetim.backend.dto.supplier.response.PurchasePaymentResponse;
import com.firinyonetim.backend.dto.supplier.request.PurchaseRequest;
import com.firinyonetim.backend.dto.supplier.response.PurchaseResponse;
import com.firinyonetim.backend.service.supplier.PurchaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchases")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('YONETICI', 'DEVELOPER', 'MUHASEBE')")
public class PurchaseController {

    private final PurchaseService purchaseService;

    @PostMapping
    public ResponseEntity<PurchaseResponse> createPurchase(@Valid @RequestBody PurchaseRequest request) {
        return new ResponseEntity<>(purchaseService.createPurchase(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<PurchaseResponse>> getAllPurchases() {
        return ResponseEntity.ok(purchaseService.getAllPurchases());
    }

    @PostMapping("/payments")
    public ResponseEntity<PurchasePaymentResponse> createPayment(@Valid @RequestBody PurchasePaymentRequest request) {
        return new ResponseEntity<>(purchaseService.createPayment(request), HttpStatus.CREATED);
    }

    @GetMapping("/payments")
    public ResponseEntity<List<PurchasePaymentResponse>> getAllPayments() {
        return ResponseEntity.ok(purchaseService.getAllPayments());
    }
}