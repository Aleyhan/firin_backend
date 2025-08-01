// src/main/java/com/firinyonetim/backend/service/ReportService.java
package com.firinyonetim.backend.service;

import com.firinyonetim.backend.dto.report.ProductStockSummaryDto;
import com.firinyonetim.backend.dto.report.RouteStockSummaryDto;
import com.firinyonetim.backend.dto.report.ShipmentStockSummaryDto;
import com.firinyonetim.backend.dto.report.StockSummaryResponseDto;
import com.firinyonetim.backend.dto.route.*;
import com.firinyonetim.backend.entity.*;
import com.firinyonetim.backend.repository.RouteAssignmentRepository;
import com.firinyonetim.backend.repository.ShipmentRepository;
import com.firinyonetim.backend.repository.TransactionRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

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
        List<Customer> customersInRoute = routeAssignmentRepository.findByRouteIdOrderByDeliveryOrderAsc(routeId)
                .stream().map(RouteAssignment::getCustomer).collect(Collectors.toList());

        List<Shipment> shipments = shipmentRepository.findByRouteIdAndShipmentDateWithDetails(routeId, date);

        if (customersInRoute.isEmpty()) {
            return new DailyRouteLedgerResponseDto();
        }

        List<Long> customerIds = customersInRoute.stream().map(Customer::getId).collect(Collectors.toList());

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

            // YENİ: Toplam satış tutarını hesapla
            BigDecimal totalSales = customerTransactions.stream()
                    .flatMap(t -> t.getItems().stream())
                    .filter(item -> item.getType() == ItemType.SATIS)
                    .map(TransactionItem::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            row.setTotalSalesAmount(totalSales); // Hesaplanan değeri DTO'ya set et

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

    @Transactional(readOnly = true)
    public StockSummaryResponseDto getStockSummary(LocalDate date, List<Long> routeIds, List<Long> driverIds) {
        Specification<Shipment> spec = (root, query, cb) -> {
            query.distinct(true);
            root.fetch("items").fetch("product");
            root.fetch("route");
            root.fetch("driver");

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("shipmentDate"), date));
            if (!CollectionUtils.isEmpty(routeIds)) {
                predicates.add(root.get("route").get("id").in(routeIds));
            }
            if (!CollectionUtils.isEmpty(driverIds)) {
                predicates.add(root.get("driver").get("id").in(driverIds));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<Shipment> shipments = shipmentRepository.findAll(spec);
        if (shipments.isEmpty()) {
            return new StockSummaryResponseDto();
        }

        List<Long> shipmentIds = shipments.stream().map(Shipment::getId).collect(Collectors.toList());
        List<Transaction> transactions = transactionRepository.findAll(
                (root, query, cb) -> root.get("shipment").get("id").in(shipmentIds)
        );

        Map<Long, List<Transaction>> transactionsByShipmentId = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.APPROVED)
                .collect(Collectors.groupingBy(t -> t.getShipment().getId()));

        StockSummaryResponseDto response = new StockSummaryResponseDto();
        Map<Long, ProductStockSummaryDto> cumulativeProductMap = new HashMap<>();
        Map<Long, RouteStockSummaryDto> routeSummaryMap = new HashMap<>();

        for (Shipment shipment : shipments) {
            Route route = shipment.getRoute();
            RouteStockSummaryDto routeSummary = routeSummaryMap.computeIfAbsent(route.getId(), k -> {
                RouteStockSummaryDto dto = new RouteStockSummaryDto();
                dto.setRouteId(route.getId());
                dto.setRouteName(route.getName());
                return dto;
            });

            ShipmentStockSummaryDto shipmentSummaryDto = new ShipmentStockSummaryDto();
            shipmentSummaryDto.setShipmentId(shipment.getId());
            shipmentSummaryDto.setSequenceNumber(shipment.getSequenceNumber());
            shipmentSummaryDto.setDriverName(shipment.getDriver().getName() + " " + shipment.getDriver().getSurname());
            shipmentSummaryDto.setStatus(shipment.getStatus());

            Map<Long, ProductStockSummaryDto> shipmentProductMap = new HashMap<>();
            List<Transaction> shipmentTransactions = transactionsByShipmentId.getOrDefault(shipment.getId(), Collections.emptyList());

            for (ShipmentItem item : shipment.getItems()) {
                Product product = item.getProduct();
                ProductStockSummaryDto summary = getOrCreateSummary(product, shipmentProductMap);
                summary.setTotalUnitsTaken(summary.getTotalUnitsTaken() + item.getTotalUnitsTaken());
                if (shipment.getStatus() == ShipmentStatus.COMPLETED) {
                    int returnedToBakery = (item.getTotalDailyUnitsReturned() != null ? item.getTotalDailyUnitsReturned() : 0)
                            + (item.getTotalReturnUnitsTaken() != null ? item.getTotalReturnUnitsTaken() : 0);
                    summary.setTotalUnitsReturnedToBakery(summary.getTotalUnitsReturnedToBakery() + returnedToBakery);
                }
            }

            for (Transaction transaction : shipmentTransactions) {
                for (TransactionItem item : transaction.getItems()) {
                    Product product = item.getProduct();
                    ProductStockSummaryDto summary = getOrCreateSummary(product, shipmentProductMap);
                    if (item.getType() == ItemType.SATIS) {
                        summary.setTotalUnitsSold(summary.getTotalUnitsSold() + item.getQuantity());
                    } else {
                        summary.setTotalUnitsReturnedByCustomer(summary.getTotalUnitsReturnedByCustomer() + item.getQuantity());
                    }
                }
            }

            for (ProductStockSummaryDto shipmentProductSummary : shipmentProductMap.values()) {
                Product product = new Product();
                product.setId(shipmentProductSummary.getProductId());
                product.setName(shipmentProductSummary.getProductName());

                ProductStockSummaryDto cumulativeSummary = getOrCreateSummary(product, cumulativeProductMap);
                ProductStockSummaryDto routeCumulativeSummary = getOrCreateSummary(product, routeSummary.getCumulativeProductSummaries());

                cumulativeSummary.setTotalUnitsTaken(cumulativeSummary.getTotalUnitsTaken() + shipmentProductSummary.getTotalUnitsTaken());
                cumulativeSummary.setTotalUnitsSold(cumulativeSummary.getTotalUnitsSold() + shipmentProductSummary.getTotalUnitsSold());
                cumulativeSummary.setTotalUnitsReturnedByCustomer(cumulativeSummary.getTotalUnitsReturnedByCustomer() + shipmentProductSummary.getTotalUnitsReturnedByCustomer());
                cumulativeSummary.setTotalUnitsReturnedToBakery(cumulativeSummary.getTotalUnitsReturnedToBakery() + shipmentProductSummary.getTotalUnitsReturnedToBakery());

                routeCumulativeSummary.setTotalUnitsTaken(routeCumulativeSummary.getTotalUnitsTaken() + shipmentProductSummary.getTotalUnitsTaken());
                routeCumulativeSummary.setTotalUnitsSold(routeCumulativeSummary.getTotalUnitsSold() + shipmentProductSummary.getTotalUnitsSold());
                routeCumulativeSummary.setTotalUnitsReturnedByCustomer(routeCumulativeSummary.getTotalUnitsReturnedByCustomer() + shipmentProductSummary.getTotalUnitsReturnedByCustomer());
                routeCumulativeSummary.setTotalUnitsReturnedToBakery(routeCumulativeSummary.getTotalUnitsReturnedToBakery() + shipmentProductSummary.getTotalUnitsReturnedToBakery());
            }

            shipmentSummaryDto.getProductSummaries().addAll(shipmentProductMap.values());
            routeSummary.getShipmentSummaries().add(shipmentSummaryDto);
        }

        response.getCumulativeSummary().addAll(cumulativeProductMap.values());
        response.getRouteBasedSummary().addAll(routeSummaryMap.values());

        response.getCumulativeSummary().forEach(this::calculateVariance);
        response.getRouteBasedSummary().forEach(rs -> {
            rs.getCumulativeProductSummaries().forEach(this::calculateVariance);
            rs.getShipmentSummaries().forEach(ss -> ss.getProductSummaries().forEach(this::calculateVariance));
        });

        return response;
    }

    private ProductStockSummaryDto createNewSummary(Product product) {
        ProductStockSummaryDto dto = new ProductStockSummaryDto();
        dto.setProductId(product.getId());
        dto.setProductName(product.getName());
        return dto;
    }

    private ProductStockSummaryDto getOrCreateSummary(Product product, Map<Long, ProductStockSummaryDto> map) {
        return map.computeIfAbsent(product.getId(), k -> createNewSummary(product));
    }

    private ProductStockSummaryDto getOrCreateSummary(Product product, List<ProductStockSummaryDto> list) {
        return list.stream()
                .filter(s -> s.getProductId().equals(product.getId()))
                .findFirst()
                .orElseGet(() -> {
                    ProductStockSummaryDto newDto = createNewSummary(product);
                    list.add(newDto);
                    return newDto;
                });
    }

    private void calculateVariance(ProductStockSummaryDto summary) {
        int expectedReturn = summary.getTotalUnitsTaken() - summary.getTotalUnitsSold() + summary.getTotalUnitsReturnedByCustomer();
        summary.setVariance(summary.getTotalUnitsReturnedToBakery() - expectedReturn);
    }
}