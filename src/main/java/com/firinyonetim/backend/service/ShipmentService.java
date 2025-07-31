// src/main/java/com/firinyonetim/backend/service/ShipmentService.java
package com.firinyonetim.backend.service;

import com.firinyonetim.backend.dto.PagedResponseDto;
import com.firinyonetim.backend.dto.route.RouteShipmentProductSummaryDto;
import com.firinyonetim.backend.dto.route.RouteShipmentSummaryDto;
import com.firinyonetim.backend.dto.shipment.request.ShipmentCreateRequest;
import com.firinyonetim.backend.dto.shipment.request.ShipmentEndRequest;
import com.firinyonetim.backend.dto.shipment.request.ShipmentItemEndRequest;
import com.firinyonetim.backend.dto.shipment.request.ShipmentItemRequest;
import com.firinyonetim.backend.dto.shipment.response.ShipmentItemReportDto;
import com.firinyonetim.backend.dto.shipment.response.ShipmentReportResponse;
import com.firinyonetim.backend.dto.shipment.response.ShipmentResponse;
import com.firinyonetim.backend.entity.*;
import com.firinyonetim.backend.exception.ResourceNotFoundException;
import com.firinyonetim.backend.mapper.ShipmentMapper;
import com.firinyonetim.backend.mapper.ShipmentReportMapper;
import com.firinyonetim.backend.repository.*;
import jakarta.persistence.criteria.Predicate; // YENİ
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort; // YENİ
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.firinyonetim.backend.dto.shipment.request.ShipmentItemUpdateRequest;
import com.firinyonetim.backend.dto.shipment.request.ShipmentUpdateRequest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator; // YENİ
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

        Map<Long, ShipmentItem> shipmentItemMap = shipment.getItems().stream()
                .collect(Collectors.toMap(item -> item.getProduct().getId(), Function.identity()));

        for (ShipmentItemEndRequest itemEndRequest : request.getItems()) {
            ShipmentItem item = shipmentItemMap.get(itemEndRequest.getProductId());
            if (item != null) {
                Product product = item.getProduct();
                if (product.getUnitsPerCrate() == null || product.getUnitsPerCrate() <= 0) {
                    throw new IllegalStateException("Ürün '" + product.getName() + "' için kasa adedi tanımlanmamış.");
                }

                item.setDailyCratesReturned(itemEndRequest.getDailyCratesReturned());
                item.setDailyUnitsReturned(itemEndRequest.getDailyUnitsReturned());
                int totalDailyReturned = (itemEndRequest.getDailyCratesReturned() * product.getUnitsPerCrate()) + itemEndRequest.getDailyUnitsReturned();
                item.setTotalDailyUnitsReturned(totalDailyReturned);

                item.setReturnCratesTaken(itemEndRequest.getReturnCratesTaken());
                item.setReturnUnitsTaken(itemEndRequest.getReturnUnitsTaken());
                int totalReturnTaken = (itemEndRequest.getReturnCratesTaken() * product.getUnitsPerCrate()) + itemEndRequest.getReturnUnitsTaken();
                item.setTotalReturnUnitsTaken(totalReturnTaken);
            }
        }

        shipmentRepository.save(shipment);
    }

    // METOT TAMAMEN YENİLENDİ
    @Transactional(readOnly = true)
    public PagedResponseDto<ShipmentReportResponse> searchShipments(LocalDate startDate, LocalDate endDate, Long routeId, Long driverId, ShipmentStatus status, Pageable pageable) {
        Specification<Shipment> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("shipmentDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("shipmentDate"), endDate));
            }
            if (routeId != null) {
                predicates.add(cb.equal(root.get("route").get("id"), routeId));
            }
            if (driverId != null) {
                predicates.add(cb.equal(root.get("driver").get("id"), driverId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Shipment> shipmentPage = shipmentRepository.findAll(spec, pageable);
        List<Long> ids = shipmentPage.getContent().stream().map(Shipment::getId).collect(Collectors.toList());

        if (ids.isEmpty()) {
            return new PagedResponseDto<>(Collections.emptyList(), shipmentPage.getNumber(), shipmentPage.getTotalElements(), shipmentPage.getTotalPages());
        }

        List<Shipment> shipmentsWithDetails = shipmentRepository.findByIdsWithDetails(ids);

        // Orijinal sıralamayı korumak için Map kullan ve sırala
        Map<Long, Shipment> detailsMap = shipmentsWithDetails.stream()
                .collect(Collectors.toMap(Shipment::getId, Function.identity()));

        List<Shipment> sortedShipments = ids.stream()
                .map(detailsMap::get)
                .collect(Collectors.toList());

        List<ShipmentReportResponse> dtoList = sortedShipments.stream()
                .map(shipmentReportMapper::toResponse)
                .collect(Collectors.toList());

        Page<ShipmentReportResponse> dtoPage = new PageImpl<>(dtoList, pageable, shipmentPage.getTotalElements());
        return new PagedResponseDto<>(dtoPage);
    }

    @Transactional(readOnly = true)
    public ShipmentReportResponse getShipmentReportById(Long shipmentId) {
        Shipment shipment = shipmentRepository.findByIdWithDetails(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with id: " + shipmentId));

        ShipmentReportResponse response = shipmentReportMapper.toResponse(shipment);

        List<Transaction> approvedTransactions = transactionRepository.findByShipmentId(shipmentId).stream()
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

        List<ShipmentItemReportDto> itemDtos = shipment.getItems().stream().map(item -> {
            ShipmentItemReportDto dto = new ShipmentItemReportDto();
            Product product = item.getProduct();

            dto.setProductId(product.getId());
            dto.setProductName(product.getName());
            dto.setCratesTaken(item.getCratesTaken());
            dto.setUnitsTaken(item.getUnitsTaken());
            dto.setTotalUnitsTaken(item.getTotalUnitsTaken());
            dto.setDailyCratesReturned(item.getDailyCratesReturned());
            dto.setDailyUnitsReturned(item.getDailyUnitsReturned());
            dto.setTotalDailyUnitsReturned(item.getTotalDailyUnitsReturned());
            dto.setReturnCratesTaken(item.getReturnCratesTaken());
            dto.setReturnUnitsTaken(item.getReturnUnitsTaken());
            dto.setTotalReturnUnitsTaken(item.getTotalReturnUnitsTaken());

            int totalSold = salesMap.getOrDefault(product.getId(), 0);
            int totalReturnedByCustomer = returnsMap.getOrDefault(product.getId(), 0);
            int expectedInVehicle = item.getTotalUnitsTaken() - totalSold + totalReturnedByCustomer;

            dto.setTotalUnitsSold(totalSold);
            dto.setTotalUnitsReturnedByCustomer(totalReturnedByCustomer);
            dto.setExpectedUnitsInVehicle(expectedInVehicle);

            if (shipment.getStatus() == ShipmentStatus.COMPLETED && item.getTotalDailyUnitsReturned() != null) {
                dto.setVariance(item.getTotalDailyUnitsReturned() - expectedInVehicle);
            }

            return dto;
        }).collect(Collectors.toList());

        response.setItems(itemDtos);
        return response;
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
                int totalReturned = (item.getTotalDailyUnitsReturned() != null ? item.getTotalDailyUnitsReturned() : 0)
                        + (item.getTotalReturnUnitsTaken() != null ? item.getTotalReturnUnitsTaken() : 0);
                summaryDto.setTotalUnitsReturned(summaryDto.getTotalUnitsReturned() + totalReturned);
            }
        }

        RouteShipmentSummaryDto result = new RouteShipmentSummaryDto();
        result.setTotalShipments(shipments.size());
        result.setProductSummaries(new ArrayList<>(productSummaryMap.values()));

        return result;
    }

    // YENİ METOT
    @Transactional
    public ShipmentReportResponse updateShipment(Long shipmentId, ShipmentUpdateRequest request) {
        Shipment shipment = shipmentRepository.findByIdWithDetails(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with id: " + shipmentId));

        shipment.setStartNotes(request.getStartNotes());
        shipment.setEndNotes(request.getEndNotes());

        Map<Long, ShipmentItem> shipmentItemMap = shipment.getItems().stream()
                .collect(Collectors.toMap(item -> item.getProduct().getId(), Function.identity()));

        for (ShipmentItemUpdateRequest itemUpdateRequest : request.getItems()) {
            ShipmentItem item = shipmentItemMap.get(itemUpdateRequest.getProductId());
            if (item != null) {
                Product product = item.getProduct();
                if (product.getUnitsPerCrate() == null || product.getUnitsPerCrate() <= 0) {
                    throw new IllegalStateException("Ürün '" + product.getName() + "' için kasa adedi tanımlanmamış.");
                }

                // Başlangıç stoklarını güncelle ve toplamı yeniden hesapla
                item.setCratesTaken(itemUpdateRequest.getCratesTaken());
                item.setUnitsTaken(itemUpdateRequest.getUnitsTaken());
                item.setTotalUnitsTaken((itemUpdateRequest.getCratesTaken() * product.getUnitsPerCrate()) + itemUpdateRequest.getUnitsTaken());

                // Bitiş stoklarını güncelle ve toplamları yeniden hesapla
                item.setDailyCratesReturned(itemUpdateRequest.getDailyCratesReturned());
                item.setDailyUnitsReturned(itemUpdateRequest.getDailyUnitsReturned());
                if (itemUpdateRequest.getDailyCratesReturned() != null && itemUpdateRequest.getDailyUnitsReturned() != null) {
                    item.setTotalDailyUnitsReturned((itemUpdateRequest.getDailyCratesReturned() * product.getUnitsPerCrate()) + itemUpdateRequest.getDailyUnitsReturned());
                } else {
                    item.setTotalDailyUnitsReturned(null);
                }

                item.setReturnCratesTaken(itemUpdateRequest.getReturnCratesTaken());
                item.setReturnUnitsTaken(itemUpdateRequest.getReturnUnitsTaken());
                if (itemUpdateRequest.getReturnCratesTaken() != null && itemUpdateRequest.getReturnUnitsTaken() != null) {
                    item.setTotalReturnUnitsTaken((itemUpdateRequest.getReturnCratesTaken() * product.getUnitsPerCrate()) + itemUpdateRequest.getReturnUnitsTaken());
                } else {
                    item.setTotalReturnUnitsTaken(null);
                }
            }
        }

        Shipment savedShipment = shipmentRepository.save(shipment);
        // Güncellenmiş ve yeniden hesaplanmış veriyi döndür
        return getShipmentReportById(savedShipment.getId());
    }


}