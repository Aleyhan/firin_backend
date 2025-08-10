package com.firinyonetim.backend.controller;

import com.firinyonetim.backend.dto.product.request.ProductCreateRequest;
import com.firinyonetim.backend.dto.product.request.ProductUpdateRequest;
import com.firinyonetim.backend.dto.product.response.AffectedCustomerDto;
import com.firinyonetim.backend.dto.product.response.ProductResponse;
import com.firinyonetim.backend.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('YONETICI')" + " or hasRole('MUHASEBE')") // YENİ: SOFOR rolü de erişim izni verildi
public class ProductController {
    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        return new ResponseEntity<>(productService.createProduct(request), HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('YONETICI')" + " or hasRole('MUHASEBE') "+" OR hasRole('SOFOR')") // YENİ: SOFOR rolü de erişim izni verildi
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long productId) {
        return ResponseEntity.ok(productService.getProductById(productId));
    }

    @PutMapping("/{productId}")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable Long productId, @Valid @RequestBody ProductUpdateRequest request) {
        return ResponseEntity.ok(productService.updateProduct(productId, request));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long productId) {
        productService.deleteProduct(productId);
        return ResponseEntity.noContent().build();
    }

    // YENİ ENDPOINT
    @GetMapping("/{productId}/customers-with-special-price")
    public ResponseEntity<List<AffectedCustomerDto>> getCustomersWithSpecialPrice(@PathVariable Long productId) {
        return ResponseEntity.ok(productService.getCustomersWithSpecialPriceForProduct(productId));
    }


}