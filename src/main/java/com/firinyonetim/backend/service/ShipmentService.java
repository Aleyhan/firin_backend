// src/main/java/com/firinyonetim/backend/service/ShipmentService.java
package com.firinyonetim.backend.service;

import com.firinyonetim.backend.dto.shipment.request.ShipmentCreateRequest;
import com.firinyonetim.backend.dto.shipment.request.ShipmentEndRequest;
import com.firinyonetim.backend.dto.shipment.request.ShipmentItemEndRequest;
import com.firinyonetim.backend.dto.shipment.request.ShipmentItemRequest;
import com.firinyonetim.backend.dto.shipment.response.ShipmentResponse;
import com.firinyonetim.backend.entity.*;
import com.firinyonetim.backend.exception.ResourceNotFoundException;
import com.firinyonetim.backend.mapper.ShipmentMapper;
import com.firinyonetim.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    private final TransactionRepository transactionRepository; // YENİ
    private final ShipmentMapper shipmentMapper;

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
        // Sadece devam eden seferleri döndür
        return shipmentOpt
                .filter(s -> s.getStatus() == ShipmentStatus.IN_PROGRESS)
                .map(shipmentMapper::toResponse)
                .orElse(null);
    }

    // YENİ METOT
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

        // 1. Gün sonu notlarını ve durumu güncelle
        shipment.setEndNotes(request.getEndNotes());
        shipment.setStatus(ShipmentStatus.COMPLETED);

        // 2. O sevkiyata ait tüm işlemleri çek
        List<Transaction> transactions = transactionRepository.findByShipmentId(shipmentId);

        // 3. Ürün bazında satış ve iade adetlerini hesapla
        Map<Long, Integer> salesMap = transactions.stream()
                .flatMap(t -> t.getItems().stream())
                .filter(item -> item.getType() == ItemType.SATIS)
                .collect(Collectors.groupingBy(item -> item.getProduct().getId(), Collectors.summingInt(TransactionItem::getQuantity)));

        Map<Long, Integer> returnsMap = transactions.stream()
                .flatMap(t -> t.getItems().stream())
                .filter(item -> item.getType() == ItemType.IADE)
                .collect(Collectors.groupingBy(item -> item.getProduct().getId(), Collectors.summingInt(TransactionItem::getQuantity)));

        // 4. Şoförün girdiği gün sonu stoklarını ve hesaplamaları kaydet
        Map<Long, ShipmentItem> shipmentItemMap = shipment.getItems().stream()
                .collect(Collectors.toMap(item -> item.getProduct().getId(), Function.identity()));

        for (ShipmentItemEndRequest itemEndRequest : request.getItems()) {
            ShipmentItem item = shipmentItemMap.get(itemEndRequest.getProductId());
            if (item != null) {
                Product product = item.getProduct();
                if (product.getUnitsPerCrate() == null || product.getUnitsPerCrate() <= 0) {
                    throw new IllegalStateException("Ürün '" + product.getName() + "' için kasa adedi tanımlanmamış.");
                }

                // Araçta kalanları kaydet
                item.setCratesReturned(itemEndRequest.getCratesReturned());
                item.setUnitsReturned(itemEndRequest.getUnitsReturned());
                int totalReturned = (itemEndRequest.getCratesReturned() * product.getUnitsPerCrate()) + itemEndRequest.getUnitsReturned();
                item.setTotalUnitsReturned(totalReturned);

                // Rapor için hesaplamaları yap ve kaydet
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
}