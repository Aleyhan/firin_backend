// src/main/java/com/firinyonetim/backend/ewaybill/controller/EWaybillUtilityController.java
package com.firinyonetim.backend.ewaybill.controller;

import com.firinyonetim.backend.dto.transaction.response.TransactionResponse;
import com.firinyonetim.backend.entity.Transaction;
import com.firinyonetim.backend.ewaybill.dto.request.TransactionsByIdsRequest;
import com.firinyonetim.backend.ewaybill.dto.response.TransactionProductSummaryDto;
import com.firinyonetim.backend.ewaybill.service.EWaybillUtilityService;
import com.firinyonetim.backend.mapper.TransactionMapper;
import com.firinyonetim.backend.repository.TransactionRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ewaybill-utilities")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('YONETICI', 'DEVELOPER', 'MUHASEBE')")
public class EWaybillUtilityController {

    private final EWaybillUtilityService ewaybillUtilityService;
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

    @GetMapping("/daily-summary/{customerId}")
    public ResponseEntity<List<TransactionProductSummaryDto>> getDailySummary(@PathVariable Long customerId) {
        return ResponseEntity.ok(ewaybillUtilityService.getDailyTransactionSummary(customerId));
    }

    @PostMapping("/summary-by-transactions")
    public ResponseEntity<List<TransactionProductSummaryDto>> getSummaryByTransactions(@Valid @RequestBody TransactionsByIdsRequest request) {
        return ResponseEntity.ok(ewaybillUtilityService.getSummaryByTransactionIds(request.getTransactionIds()));
    }

    // İşlem seçme modal'ı için o günkü işlemleri getiren endpoint
    @GetMapping("/daily-transactions/{customerId}")
    public ResponseEntity<List<TransactionResponse>> getDailyTransactions(@PathVariable Long customerId) {
        LocalDate today = LocalDate.now();
        List<Transaction> transactions = transactionRepository.findApprovedTransactionsByCustomerAndDate(
                customerId,
                today.atStartOfDay(),
                today.atTime(LocalTime.MAX)
        );
        return ResponseEntity.ok(transactions.stream().map(transactionMapper::toTransactionResponse).collect(Collectors.toList()));
    }
}