// src/main/java/com/firinyonetim/backend/service/ShipmentService.java
package com.firinyonetim.backend.service;

import com.firinyonetim.backend.dto.PagedResponseDto;
import com.firinyonetim.backend.dto.route.RouteShipmentProductSummaryDto;
import com.firinyonetim.backend.dto.route.RouteShipmentSummaryDto;
import com.firinyonetim.backend.dto.shipment.request.ShipmentCreateRequest;
import com.firinyonetim.backend.dto.shipment.request.ShipmentEndRequest;
import com.firinyonetim.backend.dto.shipment.request.ShipmentItemEndRequest;
import com.firinyonetim.backend.dto.shipment.request.ShipmentItemRequest;
import com.firinyonetim.backend.dto.shipment.response.ShipmentReportResponse;
import com.firinyonetim.backend.dto.shipment.response.ShipmentResponse;
import com.firinyonetim.backend.entity.*;
import com.firinyonetim.backend.exception.ResourceNotFoundException;
import com.firinyonetim.backend.mapper.ShipmentMapper;
import com.firinyonetim.backend.mapper.ShipmentReportMapper;
import com.firinyonetim.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final RouteRepository routeRepository;
    private final ProductRepository productRepository;
    private final TransactionRepository transactionRepository;
    private final ShipmentMapper shipmentMapper;
    private final ShipmentReportMapper shipmentReportMapper;

    // ... createShipment, getTodaysShipmentForRoute, endShipment metotları aynı ...
    @Transactional
    public void createShipment(ShipmentCreateRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Route route = routeRepository.findById(request.getRouteId())
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + request.getRouteId()));

        int sequence = shipmentRepository.countByRouteIdAndShipmentDate(route.getId(), LocalDate.now()) + 1;

        Shipment shipment = new Shipment();
        shipment.setRoute(route);
        shipment.setDriver(currentUser);
        shipment.setShipmentDate(LocalDate.now());
        shipment.setSequenceNumber(sequence);
        shipment.setStartNotes(request.getStartNotes());

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (ShipmentItemRequest itemRequest : request.getItems()) {
                Product product = productRepository.findById(itemRequest.getProductId())
                        .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + itemRequest.getProductId()));

                if (product.getUnitsPerCrate() == null || product.getUnitsPerCrate() <= 0) {
                    throw new IllegalStateException("Ürün '" + product.getName() + "' için kasa adedi tanımlanmamış.");
                }

                ShipmentItem item = new ShipmentItem();
                item.setShipment(shipment);
                item.setProduct(product);
                item.setCratesTaken(itemRequest.getCratesTaken());
                item.setUnitsTaken(itemRequest.getUnitsTaken());

                int totalUnits = (itemRequest.getCratesTaken() * product.getUnitsPerCrate()) + itemRequest.getUnitsTaken();
                item.setTotalUnitsTaken(totalUnits);

                shipment.getItems().add(item);
            }
        }

        shipmentRepository.save(shipment);
    }

    @Transactional(readOnly = true)
    public ShipmentResponse getTodaysShipmentForRoute(Long routeId) {
        Optional<Shipment> shipmentOpt = shipmentRepository.findTopByRouteIdAndShipmentDateOrderBySequenceNumberDesc(routeId, LocalDate.now());
        return shipmentOpt
                .filter(s -> s.getStatus() == ShipmentStatus.IN_PROGRESS)
                .map(shipmentMapper::toResponse)
                .orElse(null);
    }

    @Transactional
    public void endShipment(Long shipmentId, ShipmentEndRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with id: " + shipmentId));

        if (!shipment.getDriver().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Bu sevkiyatı sonlandırma yetkiniz yok.");
        }

        if (shipment.getStatus() == ShipmentStatus.COMPLETED) {
            throw new IllegalStateException("Bu sevkiyat zaten tamamlanmış.");
        }

        shipment.setEndNotes(request.getEndNotes());
        shipment.setStatus(ShipmentStatus.COMPLETED);

        List<Transaction> allTransactions = transactionRepository.findByShipmentId(shipmentId);

        List<Transaction> approvedTransactions = allTransactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.APPROVED)
                .collect(Collectors.toList());

        Map<Long, Integer> salesMap = approvedTransactions.stream()
                .flatMap(t -> t.getItems().stream())
                .filter(item -> item.getType() == ItemType.SATIS)
                .collect(Collectors.groupingBy(item -> item.getProduct().getId(), Collectors.summingInt(TransactionItem::getQuantity)));

        Map<Long, Integer> returnsMap = approvedTransactions.stream()
                .flatMap(t -> t.getItems().stream())
                .filter(item -> item.getType() == ItemType.IADE)
                .collect(Collectors.groupingBy(item -> item.getProduct().getId(), Collectors.summingInt(TransactionItem::getQuantity)));

        Map<Long, ShipmentItem> shipmentItemMap = shipment.getItems().stream()
                .collect(Collectors.toMap(item -> item.getProduct().getId(), Function.identity()));

        for (ShipmentItemEndRequest itemEndRequest : request.getItems()) {
            ShipmentItem item = shipmentItemMap.get(itemEndRequest.getProductId());
            if (item != null) {
                Product product = item.getProduct();
                if (product.getUnitsPerCrate() == null || product.getUnitsPerCrate() <= 0) {
                    throw new IllegalStateException("Ürün '" + product.getName() + "' için kasa adedi tanımlanmamış.");
                }

                item.setCratesReturned(itemEndRequest.getCratesReturned());
                item.setUnitsReturned(itemEndRequest.getUnitsReturned());
                int totalReturned = (itemEndRequest.getCratesReturned() * product.getUnitsPerCrate()) + itemEndRequest.getUnitsReturned();
                item.setTotalUnitsReturned(totalReturned);

                int totalSold = salesMap.getOrDefault(product.getId(), 0);
                int totalReturnedByCustomer = returnsMap.getOrDefault(product.getId(), 0);
                int expectedInVehicle = item.getTotalUnitsTaken() - totalSold + totalReturnedByCustomer;
                int variance = totalReturned - expectedInVehicle;

                item.setTotalUnitsSold(totalSold);
                item.setTotalUnitsReturnedByCustomer(totalReturnedByCustomer);
                item.setExpectedUnitsInVehicle(expectedInVehicle);
                item.setVariance(variance);
            }
        }

        shipmentRepository.save(shipment);
    }

    // METOT GÜNCELLENDİ
    @Transactional(readOnly = true)
    public PagedResponseDto<ShipmentReportResponse> searchShipments(LocalDate startDate, LocalDate endDate, Long routeId, Long driverId, ShipmentStatus status, Pageable pageable) {
        Page<Shipment> shipmentPage = shipmentRepository.searchShipments(startDate, endDate, routeId, driverId, status, pageable);
        Page<ShipmentReportResponse> dtoPage = shipmentPage.map(shipmentReportMapper::toResponse);
        return new PagedResponseDto<>(dtoPage);
    }

    @Transactional(readOnly = true)
    public ShipmentReportResponse getShipmentReportById(Long shipmentId) {
        Shipment shipment = shipmentRepository.findByIdWithDetails(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with id: " + shipmentId));
        return shipmentReportMapper.toResponse(shipment);
    }

    @Transactional(readOnly = true)
    public RouteShipmentSummaryDto getShipmentSummaryForRouteAndDate(Long routeId, LocalDate date) {
        List<Shipment> shipments = shipmentRepository.findByRouteIdAndShipmentDateWithDetails(routeId, date);

        if (shipments.isEmpty()) {
            return null;
        }

        Map<Product, RouteShipmentProductSummaryDto> productSummaryMap = new HashMap<>();

        for (Shipment shipment : shipments) {
            for (ShipmentItem item : shipment.getItems()) {
                Product product = item.getProduct();
                RouteShipmentProductSummaryDto summaryDto = productSummaryMap.computeIfAbsent(product, p -> {
                    RouteShipmentProductSummaryDto newDto = new RouteShipmentProductSummaryDto();
                    newDto.setProductId(p.getId());
                    newDto.setProductName(p.getName());
                    return newDto;
                });

                summaryDto.setTotalUnitsTaken(summaryDto.getTotalUnitsTaken() + item.getTotalUnitsTaken());
                if (item.getTotalUnitsReturned() != null) {
                    summaryDto.setTotalUnitsReturned(summaryDto.getTotalUnitsReturned() + item.getTotalUnitsReturned());
                }
            }
        }

        RouteShipmentSummaryDto result = new RouteShipmentSummaryDto();
        result.setTotalShipments(shipments.size());
        result.setProductSummaries(new ArrayList<>(productSummaryMap.values()));

        return result;
    }
}