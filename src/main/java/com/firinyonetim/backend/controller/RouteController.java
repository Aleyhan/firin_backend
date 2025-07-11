package com.firinyonetim.backend.controller;

import com.firinyonetim.backend.dto.customer.response.CustomerResponse;
import com.firinyonetim.backend.dto.route.request.RouteCreateRequest;
import com.firinyonetim.backend.dto.route.response.RouteResponse;
import com.firinyonetim.backend.service.RouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('YONETICI')")
public class RouteController {

    private final RouteService routeService;

    @PostMapping
    public ResponseEntity<RouteResponse> createRoute(@Valid @RequestBody RouteCreateRequest request) {
        return new ResponseEntity<>(routeService.createRoute(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<RouteResponse>> getAllRoutes() {
        return ResponseEntity.ok(routeService.getAllRoutes());
    }

    @DeleteMapping("/{routeId}")
    public ResponseEntity<Void> deleteRoute(@PathVariable Long routeId) {
        routeService.deleteRoute(routeId);
        return ResponseEntity.noContent().build();
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

    @DeleteMapping("/{routeId}/customers/{customerId}")
    public ResponseEntity<Void> removeCustomerFromRoute(@PathVariable Long routeId, @PathVariable Long customerId) {
        routeService.removeCustomerFromRoute(routeId, customerId);
        return ResponseEntity.noContent().build();
    }
}