// src/main/java/com/firinyonetim/backend/controller/DriverController.java
package com.firinyonetim.backend.controller;

import com.firinyonetim.backend.dto.customer.response.CustomerResponse;
import com.firinyonetim.backend.dto.driver.response.DriverCustomerResponse; // YENİ
import com.firinyonetim.backend.dto.driver.response.DriverDailyCustomerSummaryDto;
import com.firinyonetim.backend.dto.route.response.RouteResponse;
import com.firinyonetim.backend.dto.shipment.request.ShipmentCreateRequest;
import com.firinyonetim.backend.dto.shipment.request.ShipmentEndRequest;
import com.firinyonetim.backend.dto.shipment.response.ShipmentResponse;
import com.firinyonetim.backend.dto.transaction.request.TransactionCreateRequest;
import com.firinyonetim.backend.dto.transaction.request.TransactionUpdateRequest;
import com.firinyonetim.backend.dto.transaction.response.TransactionResponse;
import com.firinyonetim.backend.entity.User;
import com.firinyonetim.backend.service.RouteService;
import com.firinyonetim.backend.service.ShipmentService;
import com.firinyonetim.backend.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/driver")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SOFOR')")
public class DriverController {

    private final RouteService routeService;
    private final TransactionService transactionService;
    private final ShipmentService shipmentService;

    @GetMapping("/my-routes")
    public ResponseEntity<List<RouteResponse>> getMyRoutes(@AuthenticationPrincipal User driver) {
        return ResponseEntity.ok(routeService.getDriverRoutes(driver.getId()));
    }

    // DEĞİŞİKLİK BURADA
    @GetMapping("/routes/{routeId}/customers")
    public ResponseEntity<List<DriverCustomerResponse>> getCustomersForRoute(@PathVariable Long routeId) {
        return ResponseEntity.ok(routeService.getCustomersByRouteForDriver(routeId));
    }

    @PostMapping("/transactions")
    public ResponseEntity<TransactionResponse> createTransaction(@Valid @RequestBody TransactionCreateRequest request) {
        return new ResponseEntity<>(transactionService.createPendingTransaction(request), HttpStatus.CREATED);
    }

    @PutMapping("/transactions/{transactionId}")
    public ResponseEntity<TransactionResponse> updateTransaction(
            @PathVariable Long transactionId,
            @Valid @RequestBody TransactionUpdateRequest request,
            @AuthenticationPrincipal User driver) {
        return ResponseEntity.ok(transactionService.updatePendingTransaction(transactionId, request, driver.getId()));
    }

    @DeleteMapping("/transactions/{transactionId}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long transactionId, @AuthenticationPrincipal User driver) {
        transactionService.deletePendingTransaction(transactionId, driver.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/customers/{customerId}/todays-summary")
    public ResponseEntity<DriverDailyCustomerSummaryDto> getTodaysSummaryForCustomer(
            @PathVariable Long customerId,
            @AuthenticationPrincipal User driver) {
        return ResponseEntity.ok(transactionService.getDriverDailySummaryForCustomer(customerId, driver.getId()));
    }

    @PostMapping("/shipments")
    public ResponseEntity<Void> createShipment(@Valid @RequestBody ShipmentCreateRequest request) {
        shipmentService.createShipment(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/routes/{routeId}/todays-shipment")
    public ResponseEntity<ShipmentResponse> getTodaysShipmentForRoute(@PathVariable Long routeId) {
        return ResponseEntity.ok(shipmentService.getTodaysShipmentForRoute(routeId));
    }

    @PostMapping("/shipments/{shipmentId}/end")
    public ResponseEntity<Void> endShipment(
            @PathVariable Long shipmentId,
            @Valid @RequestBody ShipmentEndRequest request) {
        shipmentService.endShipment(shipmentId, request);
        return ResponseEntity.ok().build();
    }
}