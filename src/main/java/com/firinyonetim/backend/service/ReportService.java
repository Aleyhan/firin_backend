// src/main/java/com/firinyonetim/backend/service/ReportService.java
package com.firinyonetim.backend.service;

import com.firinyonetim.backend.dto.route.*;
import com.firinyonetim.backend.entity.*;
import com.firinyonetim.backend.repository.RouteAssignmentRepository;
import com.firinyonetim.backend.repository.ShipmentRepository;
import com.firinyonetim.backend.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final RouteAssignmentRepository routeAssignmentRepository;
    private final ShipmentRepository shipmentRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public DailyRouteLedgerResponseDto getDailyRouteLedger(Long routeId, LocalDate date) {
        // 1. Rotadaki müşterileri ve o günkü sevkiyatları çek
        List<Customer> customersInRoute = routeAssignmentRepository.findByRouteIdOrderByDeliveryOrderAsc(routeId)
                .stream().map(RouteAssignment::getCustomer).collect(Collectors.toList());

        // DEĞİŞİKLİK BURADA: Hatalı metot adı düzeltildi.
        List<Shipment> shipments = shipmentRepository.findByRouteIdAndShipmentDateWithDetails(routeId, date);

        if (customersInRoute.isEmpty()) {
            return new DailyRouteLedgerResponseDto(); // Boş rapor döndür
        }

        List<Long> customerIds = customersInRoute.stream().map(Customer::getId).collect(Collectors.toList());

        // 2. Müşterilerin gün başı bakiyelerini hesapla
        Map<Long, BigDecimal> currentBalances = customersInRoute.stream()
                .collect(Collectors.toMap(Customer::getId, c -> Optional.ofNullable(c.getCurrentBalanceAmount()).orElse(BigDecimal.ZERO)));

        List<Transaction> todaysApprovedTransactions = transactionRepository.findTransactionsBetween(date.atStartOfDay(), date.atTime(LocalTime.MAX))
                .stream()
                .filter(t -> t.getStatus() == TransactionStatus.APPROVED && t.getRoute() != null && t.getRoute().getId().equals(routeId))
                .collect(Collectors.toList());

        Map<Long, BigDecimal> todaysBalanceChanges = calculateTodaysBalanceChanges(todaysApprovedTransactions);

        Map<Long, BigDecimal> startOfDayBalances = new HashMap<>();
        customerIds.forEach(id -> {
            BigDecimal currentBalance = currentBalances.getOrDefault(id, BigDecimal.ZERO);
            BigDecimal todayChange = todaysBalanceChanges.getOrDefault(id, BigDecimal.ZERO);
            startOfDayBalances.put(id, currentBalance.subtract(todayChange));
        });


        // ... (metodun geri kalanı aynı)
        DailyRouteLedgerResponseDto response = new DailyRouteLedgerResponseDto();
        response.setShipmentSequences(shipments.stream().map(Shipment::getSequenceNumber).collect(Collectors.toSet()));

        List<DailyRouteLedgerCustomerRowDto> customerRows = new ArrayList<>();
        for (Customer customer : customersInRoute) {
            DailyRouteLedgerCustomerRowDto row = new DailyRouteLedgerCustomerRowDto();
            row.setCustomerId(customer.getId());
            row.setCustomerCode(customer.getCustomerCode());
            row.setCustomerName(customer.getName());
            row.setStartOfDayBalance(startOfDayBalances.getOrDefault(customer.getId(), BigDecimal.ZERO));

            List<Transaction> customerTransactions = todaysApprovedTransactions.stream()
                    .filter(t -> t.getCustomer().getId().equals(customer.getId())).collect(Collectors.toList());

            Map<Long, DailyRouteLedgerProductDto> productMovementsMap = new HashMap<>();
            for (Transaction t : customerTransactions) {
                if (t.getShipment() == null) continue;

                for (TransactionItem item : t.getItems()) {
                    DailyRouteLedgerProductDto pDto = productMovementsMap.computeIfAbsent(item.getProduct().getId(), k -> {
                        DailyRouteLedgerProductDto newDto = new DailyRouteLedgerProductDto();
                        newDto.setProductId(item.getProduct().getId());
                        newDto.setProductName(item.getProduct().getName());
                        newDto.setSalesByShipment(new HashMap<>());
                        return newDto;
                    });

                    if (item.getType() == ItemType.SATIS) {
                        pDto.getSalesByShipment().merge(t.getShipment().getSequenceNumber(), item.getQuantity(), Integer::sum);
                    } else {
                        pDto.setTotalReturns(pDto.getTotalReturns() + item.getQuantity());
                    }
                }
            }
            row.setProductMovements(new ArrayList<>(productMovementsMap.values()));

            BigDecimal cash = customerTransactions.stream()
                    .flatMap(t -> t.getPayments().stream())
                    .filter(p -> p.getType() == PaymentType.NAKIT)
                    .map(TransactionPayment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal card = customerTransactions.stream()
                    .flatMap(t -> t.getPayments().stream())
                    .filter(p -> p.getType() == PaymentType.KART)
                    .map(TransactionPayment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            row.setCashPayment(cash);
            row.setCardPayment(card);
            row.setEndOfDayBalance(row.getStartOfDayBalance().add(todaysBalanceChanges.getOrDefault(customer.getId(), BigDecimal.ZERO)));
            customerRows.add(row);
        }
        response.setCustomerRows(customerRows);

        Map<Long, DailyRouteLedgerFooterDto> footerMap = new HashMap<>();
        for (Shipment s : shipments) {
            for (ShipmentItem item : s.getItems()) {
                footerMap.computeIfAbsent(item.getProduct().getId(), k -> {
                    DailyRouteLedgerFooterDto footerDto = new DailyRouteLedgerFooterDto();
                    footerDto.setProductId(item.getProduct().getId());
                    footerDto.setProductName(item.getProduct().getName());
                    footerDto.setUnitsTakenByShipment(new HashMap<>());
                    footerDto.setUnitsReturnedByShipment(new HashMap<>());
                    footerDto.setUnitsSoldByShipment(new HashMap<>());
                    footerDto.setVarianceByShipment(new HashMap<>());
                    return footerDto;
                });
            }
        }

        for (Shipment s : shipments) {
            for (ShipmentItem item : s.getItems()) {
                DailyRouteLedgerFooterDto footerDto = footerMap.get(item.getProduct().getId());
                footerDto.getUnitsTakenByShipment().merge(s.getSequenceNumber(), item.getTotalUnitsTaken(), Integer::sum);
                if (s.getStatus() == ShipmentStatus.COMPLETED && item.getTotalDailyUnitsReturned() != null) {
                    footerDto.getUnitsReturnedByShipment().merge(s.getSequenceNumber(), item.getTotalDailyUnitsReturned(), Integer::sum);
                }
            }
        }

        for (DailyRouteLedgerCustomerRowDto row : customerRows) {
            for (DailyRouteLedgerProductDto pMovement : row.getProductMovements()) {
                DailyRouteLedgerFooterDto footerDto = footerMap.get(pMovement.getProductId());
                if (footerDto != null) {
                    footerDto.setTotalReturnedByCustomer(footerDto.getTotalReturnedByCustomer() + pMovement.getTotalReturns());
                    pMovement.getSalesByShipment().forEach((seq, qty) ->
                            footerDto.getUnitsSoldByShipment().merge(seq, qty, Integer::sum));
                }
            }
        }

        footerMap.values().forEach(f -> {
            f.getUnitsTakenByShipment().forEach((seq, taken) -> {
                int sold = f.getUnitsSoldByShipment().getOrDefault(seq, 0);
                int returned = f.getUnitsReturnedByShipment().getOrDefault(seq, 0);
                int expected = taken - sold;
                f.getVarianceByShipment().put(seq, returned - expected);
            });
        });

        response.setFooterSummary(new ArrayList<>(footerMap.values()));
        response.setUniqueProducts(footerMap.values().stream().map(f -> {
            DailyRouteLedgerProductDto p = new DailyRouteLedgerProductDto();
            p.setProductId(f.getProductId());
            p.setProductName(f.getProductName());
            return p;
        }).sorted(Comparator.comparing(DailyRouteLedgerProductDto::getProductName)).collect(Collectors.toList()));

        return response;
    }

    private Map<Long, BigDecimal> calculateTodaysBalanceChanges(List<Transaction> todaysTransactions) {
        Map<Long, BigDecimal> todaysChanges = new HashMap<>();
        for (Transaction t : todaysTransactions) {
            BigDecimal change = BigDecimal.ZERO;
            for (TransactionItem i : t.getItems()) {
                change = (i.getType() == ItemType.SATIS) ? change.add(i.getTotalPrice()) : change.subtract(i.getTotalPrice());
            }
            for (TransactionPayment p : t.getPayments()) {
                change = change.subtract(p.getAmount());
            }
            todaysChanges.merge(t.getCustomer().getId(), change, BigDecimal::add);
        }
        return todaysChanges;
    }

    private Map<Long, BigDecimal> calculateBalances(List<Customer> customers, List<Transaction> transactions) {
        return new HashMap<>();
    }
}