package com.firinyonetim.backend.controller;

import com.firinyonetim.backend.dto.customer.request.CustomerCreateRequest;
import com.firinyonetim.backend.dto.customer.response.CustomerResponse;
import com.firinyonetim.backend.dto.special_price.request.SpecialPriceRequest;
import com.firinyonetim.backend.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('YONETICI')")
public class CustomerController {
    private final CustomerService customerService;

    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(@RequestBody CustomerCreateRequest request) {
        return ResponseEntity.ok(customerService.createCustomer(request));
    }

    @PostMapping("/{customerId}/special-prices")
    public ResponseEntity<CustomerResponse> addOrUpdateSpecialPrice(
            @PathVariable Long customerId,
            @RequestBody SpecialPriceRequest request) {
        return ResponseEntity.ok(customerService.addOrUpdateSpecialPrice(customerId, request));
    }
}