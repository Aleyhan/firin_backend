// src/main/java/com/firinyonetim/backend/ewaybill/service/EWaybillUtilityService.java
package com.firinyonetim.backend.ewaybill.service;

import com.firinyonetim.backend.entity.ItemType;
import com.firinyonetim.backend.entity.Transaction;
import com.firinyonetim.backend.entity.TransactionItem;
import com.firinyonetim.backend.ewaybill.dto.response.TransactionProductSummaryDto;
import com.firinyonetim.backend.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EWaybillUtilityService {

    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public List<TransactionProductSummaryDto> getDailyTransactionSummary(Long customerId) {
        LocalDate today = LocalDate.now();
        List<Transaction> transactions = transactionRepository.findApprovedTransactionsByCustomerAndDate(
                customerId,
                today.atStartOfDay(),
                today.atTime(LocalTime.MAX)
        );
        return summarizeTransactions(transactions);
    }

    @Transactional(readOnly = true)
    public List<TransactionProductSummaryDto> getSummaryByTransactionIds(List<Long> transactionIds) {
        List<Transaction> transactions = transactionRepository.findAllById(transactionIds);
        List<Transaction> approvedTransactions = transactions.stream()
                .filter(t -> t.getStatus() == com.firinyonetim.backend.entity.TransactionStatus.APPROVED)
                .collect(Collectors.toList());
        return summarizeTransactions(approvedTransactions);
    }

    private List<TransactionProductSummaryDto> summarizeTransactions(List<Transaction> transactions) {
        Map<Long, BigDecimal> productQuantities = transactions.stream()
                .flatMap(transaction -> transaction.getItems().stream())
                .collect(Collectors.groupingBy(
                        item -> item.getProduct().getId(),
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                // --- DEĞİŞİKLİK BURADA ---
                                // int olan quantity'yi BigDecimal'e çevirip işlemi yapıyoruz.
                                item -> {
                                    BigDecimal quantity = BigDecimal.valueOf(item.getQuantity());
                                    return item.getType() == ItemType.SATIS ? quantity : quantity.negate();
                                },
                                // --- DEĞİŞİKLİK SONU ---
                                BigDecimal::add
                        )
                ));

        return productQuantities.entrySet().stream()
                .filter(entry -> entry.getValue().compareTo(BigDecimal.ZERO) > 0)
                .map(entry -> {
                    TransactionItem firstItem = findFirstItemForProduct(transactions, entry.getKey());
                    String unitCode = "C62";
                    if (firstItem.getProduct().getUnit() != null && StringUtils.hasText(firstItem.getProduct().getUnit().getCode())) {
                        unitCode = firstItem.getProduct().getUnit().getCode();
                    }
                    return new TransactionProductSummaryDto(
                            entry.getKey(),
                            firstItem.getProduct().getName(),
                            unitCode,
                            entry.getValue()
                    );
                })
                .collect(Collectors.toList());
    }

    private TransactionItem findFirstItemForProduct(List<Transaction> transactions, Long productId) {
        return transactions.stream()
                .flatMap(t -> t.getItems().stream())
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow();
    }
}