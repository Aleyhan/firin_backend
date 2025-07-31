// src/main/java/com/firinyonetim/backend/service/DashboardService.java
package com.firinyonetim.backend.service;

import com.firinyonetim.backend.dto.dashboard.DailyShipmentSummaryDto; // YENİ
import com.firinyonetim.backend.dto.dashboard.DashboardDailySummaryDto;
import com.firinyonetim.backend.dto.dashboard.DashboardStatsDto;
import com.firinyonetim.backend.dto.dashboard.ProductShipmentSummaryDto; // YENİ
import com.firinyonetim.backend.dto.transaction.response.TransactionResponse;
import com.firinyonetim.backend.entity.*;
import com.firinyonetim.backend.mapper.TransactionMapper;
import com.firinyonetim.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList; // YENİ
import java.util.HashMap; // YENİ
import java.util.List;
import java.util.Map; // YENİ
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final RouteRepository routeRepository;
    private final TransactionRepository transactionRepository;
    private final ShipmentRepository shipmentRepository; // YENİ
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

    // YENİ METOT
    @Transactional(readOnly = true)
    public DailyShipmentSummaryDto getDailyShipmentSummary(LocalDate date) {
        List<Shipment> shipments = shipmentRepository.findByShipmentDateWithDetails(date);

        if (shipments.isEmpty()) {
            DailyShipmentSummaryDto emptySummary = new DailyShipmentSummaryDto();
            emptySummary.setTotalShipments(0);
            emptySummary.setProductSummaries(new ArrayList<>());
            return emptySummary;
        }

        Map<Product, ProductShipmentSummaryDto> summaryMap = new HashMap<>();

        for (Shipment shipment : shipments) {
            for (ShipmentItem item : shipment.getItems()) {
                Product product = item.getProduct();
                ProductShipmentSummaryDto productSummary = summaryMap.computeIfAbsent(product, p -> {
                    ProductShipmentSummaryDto dto = new ProductShipmentSummaryDto();
                    dto.setProductId(p.getId());
                    dto.setProductName(p.getName());
                    return dto;
                });

                productSummary.setTotalUnitsTaken(productSummary.getTotalUnitsTaken() + item.getTotalUnitsTaken());

                // Sadece tamamlanmış seferlerin geri gelenlerini topla
                if (shipment.getStatus() == ShipmentStatus.COMPLETED && item.getTotalDailyUnitsReturned() != null && item.getTotalReturnUnitsTaken() != null) {
                    int totalReturned = item.getTotalDailyUnitsReturned() + item.getTotalReturnUnitsTaken();
                    productSummary.setTotalUnitsReturned(productSummary.getTotalUnitsReturned() + totalReturned);
                }
            }
        }

        List<ProductShipmentSummaryDto> productSummaries = new ArrayList<>(summaryMap.values());
        productSummaries.forEach(summary -> summary.setNetDispatch(summary.getTotalUnitsTaken() - summary.getTotalUnitsReturned()));

        DailyShipmentSummaryDto result = new DailyShipmentSummaryDto();
        result.setTotalShipments(shipments.size());
        result.setProductSummaries(productSummaries);

        return result;
    }
}