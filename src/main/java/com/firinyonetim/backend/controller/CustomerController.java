package com.firinyonetim.backend.controller;

import com.firinyonetim.backend.dto.customer.request.CustomerCreateRequest;
import com.firinyonetim.backend.dto.customer.request.CustomerProductAssignmentRequest;
import com.firinyonetim.backend.dto.customer.request.CustomerUpdateRequest;
import com.firinyonetim.backend.dto.customer.response.CustomerProductAssignmentResponse;
import com.firinyonetim.backend.dto.customer.response.CustomerResponse;
import com.firinyonetim.backend.dto.customer.response.LastPaymentDateResponse;
import com.firinyonetim.backend.dto.route.response.RouteResponse;
import com.firinyonetim.backend.dto.tax_info.request.TaxInfoRequest;
import com.firinyonetim.backend.dto.transaction.response.TransactionResponse;
import com.firinyonetim.backend.service.CustomerService;
import com.firinyonetim.backend.service.RouteService;
import com.firinyonetim.backend.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.firinyonetim.backend.dto.address.request.AddressRequest;
import java.time.LocalDate;

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

    @PostMapping("/{customerId}/tax-info")
    @PreAuthorize("hasRole('YONETICI')")
    public ResponseEntity<CustomerResponse> createTaxInfoForCustomer(
            @PathVariable Long customerId,
            @Valid @RequestBody TaxInfoRequest taxInfoRequest) {
        return ResponseEntity.ok(customerService.createTaxInfoForCustomer(customerId, taxInfoRequest));
    }

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

    // YENİ ENDPOINT: Müşterinin hesap ekstresini (ledger) getirir.
    @GetMapping("/{customerId}/ledger")
    @PreAuthorize("hasRole('YONETICI')")
    public ResponseEntity<List<TransactionResponse>> getCustomerLedger(@PathVariable Long customerId) {
        List<TransactionResponse> transactions = transactionService.getTransactionsByCustomerId(customerId);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/{customerId}/last-payment-date")
    public ResponseEntity<LastPaymentDateResponse> getLastPaymentDate(@PathVariable Long customerId) {
        LastPaymentDateResponse response = customerService.getLastPaymentDate(customerId);
        return ResponseEntity.ok(response);
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

    // YENİ ENDPOINT: Müşterinin adresini kısmi olarak günceller.
    @PatchMapping("/{customerId}/address")
    @PreAuthorize("hasRole('YONETICI')")
    public ResponseEntity<CustomerResponse> updateCustomerAddress(
            @PathVariable Long customerId,
            @RequestBody Map<String, String> updates) {
        return ResponseEntity.ok(customerService.updateCustomerAddress(customerId, updates));
    }

    @PostMapping("/{customerId}/address")
    @PreAuthorize("hasRole('YONETICI')")
    public ResponseEntity<CustomerResponse> createAddressForCustomer(
            @PathVariable Long customerId,
            @RequestBody @Valid AddressRequest addressRequest) {
        return ResponseEntity.ok(customerService.createAddressForCustomer(customerId, addressRequest));
    }

    @PostMapping("/{customerId}/routes")
    @PreAuthorize("hasRole('YONETICI')")
    public ResponseEntity<Void> assignRoutesToCustomer(
            @PathVariable Long customerId,
            @RequestBody List<Long> routeIds) {
        customerService.assignRoutesToCustomer(customerId, routeIds);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{customerId}/workdays")
    @PreAuthorize("hasRole('YONETICI')")
    public ResponseEntity<CustomerResponse> updateCustomerWorkdays(
            @PathVariable Long customerId,
            @RequestBody List<String> workdays) {
        CustomerResponse response = customerService.updateCustomerWorkdays(customerId, workdays);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{customerId}/irsaliye-gunleri")
    @PreAuthorize("hasRole('YONETICI')")
    public ResponseEntity<CustomerResponse> updateCustomerIrsaliyeGunleri(
            @PathVariable Long customerId,
            @RequestBody List<String> irsaliyeGunleri) {
        CustomerResponse response = customerService.updateCustomerIrsaliyeGunleri(customerId, irsaliyeGunleri);
        return ResponseEntity.ok(response);
    }

    // CustomerController.java içinde...
    @PutMapping("/{customerId}/products") // PUT, çünkü bu işlem "oluştur veya güncelle" (upsert) mantığında.
    @PreAuthorize("hasRole('YONETICI')")
    public ResponseEntity<CustomerProductAssignmentResponse> assignOrUpdateProductToCustomer(
            @PathVariable Long customerId,
            @RequestBody CustomerProductAssignmentRequest request) {
        return ResponseEntity.ok(customerService.assignOrUpdateProductToCustomer(customerId, request));
    }

    // CustomerController.java içinde...

    // YENİ ENDPOINT: Bir müşteriye atanmış tüm ürünleri ve kurallarını listeler.
    @GetMapping("/{customerId}/products")
    @PreAuthorize("hasRole('YONETICI') or hasRole('SOFOR')")
    public ResponseEntity<List<CustomerProductAssignmentResponse>> getCustomerProductAssignments(@PathVariable Long customerId) {
        return ResponseEntity.ok(customerService.getCustomerProductAssignments(customerId));
    }

    @DeleteMapping("/{customerId}/products/{productId}")
    @PreAuthorize("hasRole('YONETICI')")
    public ResponseEntity<Void> removeAssignedProduct(
            @PathVariable Long customerId,
            @PathVariable Long productId) {
        customerService.removeAssignedProduct(customerId, productId);
        return ResponseEntity.noContent().build();
    }

}
