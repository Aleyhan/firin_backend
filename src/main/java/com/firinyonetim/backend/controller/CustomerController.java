package com.firinyonetim.backend.controller;

import com.firinyonetim.backend.dto.customer.request.CustomerCreateRequest;
import com.firinyonetim.backend.dto.customer.request.CustomerUpdateRequest;
import com.firinyonetim.backend.dto.customer.response.CustomerResponse;
import com.firinyonetim.backend.dto.route.response.RouteResponse;
import com.firinyonetim.backend.dto.special_price.request.SpecialPriceRequest;
import com.firinyonetim.backend.dto.transaction.response.TransactionResponse;
import com.firinyonetim.backend.service.CustomerService;
import com.firinyonetim.backend.service.RouteService;
import com.firinyonetim.backend.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")
@PreAuthorize("hasRole('YONETICI')")
public class CustomerController {
    private final CustomerService customerService;
    private final TransactionService transactionService; // YENİ: TransactionService'i inject et
    private final RouteService routeService; // YENİ: RouteService'i inject et

    public CustomerController(CustomerService customerService, TransactionService transactionService,
                              RouteService routeService) {
        this.customerService = customerService;
        this.transactionService = transactionService;
        this.routeService = routeService;
    }

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

    // ... CustomerController sınıfının içinde ...

    @PutMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable Long customerId,
            @RequestBody CustomerUpdateRequest request) {
        return ResponseEntity.ok(customerService.updateCustomer(customerId, request));
    }

    // ... CustomerController sınıfının içinde ...

    @GetMapping
    public ResponseEntity<List<CustomerResponse>> getAllCustomers() {
        return ResponseEntity.ok(customerService.getAllCustomers());
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> getCustomerById(@PathVariable Long customerId) {
        return ResponseEntity.ok(customerService.getCustomerById(customerId));
    }

    @DeleteMapping("/{customerId}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long customerId) {
        customerService.deleteCustomer(customerId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{customerId}/special-prices/{productId}")
    public ResponseEntity<Void> removeSpecialPrice(@PathVariable Long customerId, @PathVariable Long productId) {
        customerService.removeSpecialPrice(customerId, productId);
        return ResponseEntity.noContent().build();
    }

    // YENİ ENDPOINT: Müşterinin hesap ekstresini (ledger) getirir.
    @GetMapping("/{customerId}/ledger")
    @PreAuthorize("hasRole('YONETICI')")
    public ResponseEntity<List<TransactionResponse>> getCustomerLedger(@PathVariable Long customerId) {
        List<TransactionResponse> transactions = transactionService.getTransactionsByCustomerId(customerId);
        return ResponseEntity.ok(transactions);
    }
    // controller/CustomerController.java
    @GetMapping("/{customerId}/routes")
    public ResponseEntity<List<RouteResponse>> getRoutesByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(routeService.getRoutesByCustomer(customerId));
    }

    @PatchMapping("/{customerId}/update-fields")
    public ResponseEntity<CustomerResponse> updateCustomerFields(
            @PathVariable Long customerId,
            @RequestBody Map<String, Object> updates) {
        return ResponseEntity.ok(customerService.updateCustomerFields(customerId, updates));
    }

    // YENİ ENDPOINT: Müşterinin vergi bilgilerini kısmi olarak günceller.
    @PatchMapping("/{customerId}/tax-info")
    @PreAuthorize("hasRole('YONETICI')")
    public ResponseEntity<CustomerResponse> updateCustomerTaxInfo(
            @PathVariable Long customerId,
            @RequestBody Map<String, String> updates) {
        return ResponseEntity.ok(customerService.updateCustomerTaxInfo(customerId, updates));
    }

}