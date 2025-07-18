package com.firinyonetim.backend.controller;

import com.firinyonetim.backend.dto.transaction.request.TransactionCreateRequest;
import com.firinyonetim.backend.dto.transaction.request.TransactionItemPriceUpdateRequest;
import com.firinyonetim.backend.dto.transaction.request.TransactionUpdateRequest;
import com.firinyonetim.backend.dto.transaction.response.TransactionResponse;
import com.firinyonetim.backend.service.CustomerService;
import com.firinyonetim.backend.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.bind.annotation.GetMapping; // GetMapping import'u
import org.springframework.web.bind.annotation.PathVariable; // PathVariable import'u
import java.util.List; // List import'u
import org.springframework.web.bind.annotation.PostMapping; // PostMapping import'u
import org.springframework.web.bind.annotation.PutMapping; // PutMapping import'u
import org.springframework.web.bind.annotation.DeleteMapping; // DeleteMapping import'u
import org.springframework.web.bind.annotation.RequestBody; // RequestBody import'u
import org.springframework.web.bind.annotation.RequestMapping; // RequestMapping import'u
import org.springframework.web.bind.annotation.PatchMapping;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('YONETICI')")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(@Valid @RequestBody TransactionCreateRequest request) {
        return new ResponseEntity<>(transactionService.createTransaction(request), HttpStatus.CREATED);
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

    // ... TransactionController sınıfının içinde ...

    @PatchMapping("/{transactionId}/items/{itemId}/price")
    public ResponseEntity<TransactionResponse> updateTransactionItemPrice(
            @PathVariable Long transactionId,
            @PathVariable Long itemId,
            @Valid @RequestBody TransactionItemPriceUpdateRequest request) {
        return ResponseEntity.ok(transactionService.updateTransactionItemPrice(transactionId, itemId, request));
    }


}