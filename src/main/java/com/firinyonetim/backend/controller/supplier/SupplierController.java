package com.firinyonetim.backend.controller.supplier;

import com.firinyonetim.backend.dto.address.request.AddressRequest;
import com.firinyonetim.backend.dto.supplier.request.SupplierRequest;
import com.firinyonetim.backend.dto.supplier.response.SupplierResponse;
import com.firinyonetim.backend.dto.supplier.request.SupplierTaxInfoRequest;
import com.firinyonetim.backend.service.supplier.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('YONETICI', 'DEVELOPER', 'MUHASEBE')")
public class SupplierController {

    private final SupplierService supplierService;

    // ... (diğer endpoint'ler aynı) ...
    @GetMapping
    public ResponseEntity<List<SupplierResponse>> getAllSuppliers() {
        return ResponseEntity.ok(supplierService.getAllSuppliers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplierResponse> getSupplierById(@PathVariable Long id) {
        return ResponseEntity.ok(supplierService.getSupplierById(id));
    }

    @PostMapping
    public ResponseEntity<SupplierResponse> createSupplier(@Valid @RequestBody SupplierRequest request) {
        return new ResponseEntity<>(supplierService.createSupplier(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SupplierResponse> updateSupplier(@PathVariable Long id, @Valid @RequestBody SupplierRequest request) {
        return ResponseEntity.ok(supplierService.updateSupplier(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSupplier(@PathVariable Long id) {
        supplierService.deleteSupplier(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{supplierId}/update-fields")
    public ResponseEntity<SupplierResponse> updateSupplierFields(@PathVariable Long supplierId, @RequestBody Map<String, Object> updates) {
        return ResponseEntity.ok(supplierService.updateSupplierFields(supplierId, updates));
    }

    // --- ADRES ENDPOINT'LERİ GÜNCELLENDİ ---
    @PostMapping("/{supplierId}/address")
    public ResponseEntity<SupplierResponse> createAddressForSupplier(@PathVariable Long supplierId, @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(supplierService.createAddressForSupplier(supplierId, request));
    }

    @PatchMapping("/{supplierId}/address")
    public ResponseEntity<SupplierResponse> updateSupplierAddress(@PathVariable Long supplierId, @RequestBody Map<String, String> updates) {
        return ResponseEntity.ok(supplierService.updateSupplierAddress(supplierId, updates));
    }

    // --- VERGİ BİLGİSİ ENDPOINT'LERİ GÜNCELLENDİ ---
    @PostMapping("/{supplierId}/tax-info")
    public ResponseEntity<SupplierResponse> createTaxInfoForSupplier(@PathVariable Long supplierId, @Valid @RequestBody SupplierTaxInfoRequest request) {
        return ResponseEntity.ok(supplierService.createTaxInfoForSupplier(supplierId, request));
    }

    @PatchMapping("/{supplierId}/tax-info")
    public ResponseEntity<SupplierResponse> updateSupplierTaxInfo(@PathVariable Long supplierId, @RequestBody Map<String, String> updates) {
        return ResponseEntity.ok(supplierService.updateSupplierTaxInfo(supplierId, updates));
    }
}