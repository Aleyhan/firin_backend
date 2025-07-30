// src/main/java/com/firinyonetim/backend/service/DashboardService.java
package com.firinyonetim.backend.service;

import com.firinyonetim.backend.dto.dashboard.DashboardDailySummaryDto;
import com.firinyonetim.backend.dto.dashboard.DashboardStatsDto;
import com.firinyonetim.backend.dto.transaction.response.TransactionResponse;
import com.firinyonetim.backend.entity.ItemType;
import com.firinyonetim.backend.entity.PaymentType;
import com.firinyonetim.backend.entity.Transaction;
import com.firinyonetim.backend.entity.TransactionItem;
import com.firinyonetim.backend.entity.TransactionStatus; // YENİ
import com.firinyonetim.backend.mapper.TransactionMapper;
import com.firinyonetim.backend.repository.CustomerRepository;
import com.firinyonetim.backend.repository.ProductRepository;
import com.firinyonetim.backend.repository.RouteRepository;
import com.firinyonetim.backend.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final RouteRepository routeRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats() {
        long customerCount = customerRepository.count();
        long productCount = productRepository.count();
        long routeCount = routeRepository.count();
        return new DashboardStatsDto(customerCount, productCount, routeCount);
    }

    @Transactional(readOnly = true)
    public DashboardDailySummaryDto getDailySummary(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime startOfNextDay = date.plusDays(1).atStartOfDay();
        List<Transaction> transactions = transactionRepository.findTransactionsBetween(startOfDay, startOfNextDay);

        DashboardDailySummaryDto summary = new DashboardDailySummaryDto();

        // Sadece onaylanmış işlemleri filtrele
        List<Transaction> approvedTransactions = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.APPROVED)
                .collect(Collectors.toList());

        for (Transaction transaction : approvedTransactions) {
            for (TransactionItem item : transaction.getItems()) {
                if (item.getType() == ItemType.SATIS) {
                    summary.setTotalSales(summary.getTotalSales().add(item.getTotalPrice()));
                } else if (item.getType() == ItemType.IADE) {
                    summary.setTotalReturns(summary.getTotalReturns().add(item.getTotalPrice()));
                }
            }
            transaction.getPayments().forEach(payment -> {
                if (payment.getType() == PaymentType.NAKIT) {
                    summary.setTotalCashPayment(summary.getTotalCashPayment().add(payment.getAmount()));
                } else if (payment.getType() == PaymentType.KART) {
                    summary.setTotalCardPayment(summary.getTotalCardPayment().add(payment.getAmount()));
                }
            });
        }

        BigDecimal netRevenue = summary.getTotalSales().subtract(summary.getTotalReturns());
        BigDecimal totalPayments = summary.getTotalCashPayment().add(summary.getTotalCardPayment());
        BigDecimal balanceChange = netRevenue.subtract(totalPayments);

        summary.setNetRevenue(netRevenue);
        summary.setBalanceChange(balanceChange);

        return summary;
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getRecentTransactions() {
        List<Transaction> recentTransactions = transactionRepository.findTop10ByOrderByTransactionDateDesc();
        return recentTransactions.stream()
                .map(transactionMapper::toTransactionResponse)
                .collect(Collectors.toList());
    }
}