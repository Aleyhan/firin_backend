package com.firinyonetim.backend.controller;

import com.firinyonetim.backend.dto.customer.response.CustomerResponse;
import com.firinyonetim.backend.dto.route.RouteDailySummaryDto;
import com.firinyonetim.backend.dto.route.request.RouteCreateRequest;
import com.firinyonetim.backend.dto.route.request.RouteUpdateRequest;
import com.firinyonetim.backend.dto.route.response.RouteResponse;
import com.firinyonetim.backend.service.RouteService;
import com.firinyonetim.backend.service.ShipmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.firinyonetim.backend.dto.route.RouteShipmentSummaryDto;


import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('YONETICI') or hasRole('MUHASEBE')") // <<< ROL KONTROLÜ EKLENDİ
public class RouteController {

    private final RouteService routeService;
    private final ShipmentService shipmentService; // <<< ShipmentService Eklendi

    @PostMapping
    public ResponseEntity<RouteResponse> createRoute(@Valid @RequestBody RouteCreateRequest request) {
        return new ResponseEntity<>(routeService.createRoute(request), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('YONETICI', 'DEVELOPER', 'MUHASEBE', 'SOFOR')") // <<< ROL KONTROLÜ EKLENDİ
    public ResponseEntity<List<RouteResponse>> getAllRoutes() {
        return ResponseEntity.ok(routeService.getAllRoutes());
    }

    @DeleteMapping("/{routeId}")
    public ResponseEntity<Void> deleteRouteByStatus(@PathVariable Long routeId) {
        routeService.deleteRouteByStatus(routeId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{routeId}")
    @PreAuthorize("hasRole('YONETICI') or hasRole('SOFOR') or hasRole('MUHASEBE')") // <<< ROL KONTROLÜ EKLENDİ
    public ResponseEntity<RouteResponse> getRouteById(@PathVariable Long routeId) {
        return ResponseEntity.ok(routeService.getRouteById(routeId));
    }

    @PutMapping("/{routeId}")
    public ResponseEntity<RouteResponse> updateRoute(
            @PathVariable Long routeId,
            @Valid @RequestBody RouteUpdateRequest request) { // <<< BURANIN RouteUpdateRequest OLDUĞUNDAN EMİN OLUN
        return ResponseEntity.ok(routeService.updateRoute(routeId, request));
    }

    @GetMapping("/{routeId}/customers")
    public ResponseEntity<List<CustomerResponse>> getCustomersByRoute(@PathVariable Long routeId) {
        return ResponseEntity.ok(routeService.getCustomersByRoute(routeId));
    }


    @PostMapping("/{routeId}/customers")
    public ResponseEntity<Void> assignCustomerToRoute(@PathVariable Long routeId, @RequestBody Map<String, Long> payload) {
        Long customerId = payload.get("customerId");
        routeService.assignCustomerToRoute(routeId, customerId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{routeId}/customers/batch-assign")
    public ResponseEntity<Void> assignCustomersToRoute(@PathVariable Long routeId, @RequestBody List<Long> customerIds) {
        routeService.assignCustomersToRoute(routeId, customerIds);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{routeId}/customers")
    public ResponseEntity<Void> updateRouteCustomers(
            @PathVariable Long routeId,
            @RequestBody List<Long> customerIds) {
        routeService.updateRouteCustomers(routeId, customerIds);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{routeId}/customers/{customerId}")
    public ResponseEntity<Void> removeCustomerFromRoute(@PathVariable Long routeId, @PathVariable Long customerId) {
        routeService.removeCustomerFromRoute(routeId, customerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/counts")
    public ResponseEntity<Map<Long, Long>> getCustomerCountsPerRoute() { // <<< DÖNÜŞ TİPİ DEĞİŞTİ
        return ResponseEntity.ok(routeService.getCustomerCountsPerRoute());
    }

    @PatchMapping("/{routeId}/toggle-status")
        public ResponseEntity<RouteResponse> toggleRouteStatus(@PathVariable Long routeId) {
            return ResponseEntity.ok(routeService.toggleRouteStatus(routeId));
        }

    @GetMapping("/{routeId}/total-debt")
    public ResponseEntity<Double> getTotalDebtForRoute(@PathVariable Long routeId) {
        double totalDebt = routeService.getTotalDebtForRoute(routeId);
        return ResponseEntity.ok(totalDebt);
    }

    // YENİ ENDPOINT
    @GetMapping("/daily-summary")
    public ResponseEntity<List<RouteDailySummaryDto>> getDailySummaries(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(routeService.getDailySummaries(date));
    }

    // YENİ ENDPOINT
    @GetMapping("/{routeId}/shipment-summary")
    public ResponseEntity<RouteShipmentSummaryDto> getShipmentSummary(
            @PathVariable Long routeId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        RouteShipmentSummaryDto summary = shipmentService.getShipmentSummaryForRouteAndDate(routeId, date);
        if (summary == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(summary);
    }

}