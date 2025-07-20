package com.firinyonetim.backend.controller;

import com.firinyonetim.backend.dto.product.ProductGroupDto;
import com.firinyonetim.backend.service.ProductGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product-groups")
@RequiredArgsConstructor
@PreAuthorize("hasRole('YONETICI')")
public class ProductGroupController {

    private final ProductGroupService productGroupService;

    @GetMapping
    public ResponseEntity<List<ProductGroupDto>> getAllProductGroups() {
        return ResponseEntity.ok(productGroupService.getAllProductGroups());
    }

    @PostMapping
    public ResponseEntity<ProductGroupDto> createProductGroup(@RequestBody ProductGroupDto dto) {
        return new ResponseEntity<>(productGroupService.createProductGroup(dto), HttpStatus.CREATED);
    }

    // YENİ ENDPOINT
    @PutMapping("/{id}")
    public ResponseEntity<ProductGroupDto> updateProductGroup(@PathVariable Long id, @RequestBody ProductGroupDto dto) {
        return ResponseEntity.ok(productGroupService.updateProductGroup(id, dto));
    }

    // YENİ ENDPOINT
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProductGroup(@PathVariable Long id) {
        productGroupService.deleteProductGroup(id);
        return ResponseEntity.noContent().build();
    }
}