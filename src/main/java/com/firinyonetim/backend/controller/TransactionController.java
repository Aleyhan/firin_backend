// src/main/java/com/firinyonetim/backend/controller/TransactionController.java
package com.firinyonetim.backend.controller;

import com.firinyonetim.backend.dto.PagedResponseDto; // YENİ IMPORT
import com.firinyonetim.backend.dto.transaction.request.TransactionCreateRequest;
import com.firinyonetim.backend.dto.transaction.request.TransactionItemPriceUpdateRequest;
import com.firinyonetim.backend.dto.transaction.request.TransactionUpdateRequest;
import com.firinyonetim.backend.dto.transaction.response.TransactionResponse;
import com.firinyonetim.backend.entity.TransactionStatus;
import com.firinyonetim.backend.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable; // YENİ IMPORT
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('YONETICI') or hasRole('MUHASEBE')") // Rolleri genişletelim
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(@Valid @RequestBody TransactionCreateRequest request) {
        return new ResponseEntity<>(transactionService.createAndApproveTransaction(request), HttpStatus.CREATED);
    }

    @DeleteMapping("/{transactionId}")
    @PreAuthorize("hasRole('YONETICI')")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long transactionId) {
        transactionService.deleteTransaction(transactionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransactionById(@PathVariable Long transactionId) {
        return ResponseEntity.ok(transactionService.getTransactionById(transactionId));
    }

    @PatchMapping("/{transactionId}/items/{itemId}/price")
    public ResponseEntity<TransactionResponse> updateTransactionItemPrice(
            @PathVariable Long transactionId,
            @PathVariable Long itemId,
            @Valid @RequestBody TransactionItemPriceUpdateRequest request) {
        return ResponseEntity.ok(transactionService.updateTransactionItemPrice(transactionId, itemId, request));
    }

    @PutMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> updateTransaction(
            @PathVariable Long transactionId,
            @Valid @RequestBody TransactionUpdateRequest request) {
        return ResponseEntity.ok(transactionService.updateTransaction(transactionId, request));
    }

    // METOT İMZASI TAMAMEN GÜNCELLENDİ
    @GetMapping("/search")
    public ResponseEntity<PagedResponseDto<TransactionResponse>> searchTransactions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long routeId,
            @RequestParam(required = false) TransactionStatus status,
            Pageable pageable) { // YENİ PARAMETRE
        PagedResponseDto<TransactionResponse> results = transactionService.searchTransactions(startDate, endDate, customerId, routeId, status, pageable);
        return ResponseEntity.ok(results);
    }
}