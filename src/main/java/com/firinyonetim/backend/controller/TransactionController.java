package com.firinyonetim.backend.controller;

import com.firinyonetim.backend.dto.transaction.request.TransactionCreateRequest;
import com.firinyonetim.backend.dto.transaction.request.TransactionItemPriceUpdateRequest;
import com.firinyonetim.backend.dto.transaction.request.TransactionUpdateRequest;
import com.firinyonetim.backend.dto.transaction.response.TransactionResponse;
import com.firinyonetim.backend.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@PreAuthorize("hasRole('YONETICI')") // Bu controller'daki tüm işlemler yönetici yetkisi gerektirir
public class TransactionController {

    private final TransactionService transactionService;

    // DEĞİŞİKLİK: Bu metot artık işlemi direkt ONAYLANMIŞ olarak oluşturur.
    // Yönetici tarafından girilen işlemler için kullanılır.
    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(@Valid @RequestBody TransactionCreateRequest request) {
        return new ResponseEntity<>(transactionService.createAndApproveTransaction(request), HttpStatus.CREATED);
    }

    @DeleteMapping("/{transactionId}")
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

    @GetMapping("/search")
    public ResponseEntity<List<TransactionResponse>> searchTransactions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long routeId) {
        List<TransactionResponse> results = transactionService.searchTransactions(startDate, endDate, customerId, routeId);
        return ResponseEntity.ok(results);
    }
}