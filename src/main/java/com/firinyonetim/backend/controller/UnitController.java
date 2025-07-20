package com.firinyonetim.backend.controller;

import com.firinyonetim.backend.dto.product.UnitDto;
import com.firinyonetim.backend.service.UnitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/units")
@RequiredArgsConstructor
@PreAuthorize("hasRole('YONETICI')")
public class UnitController {

    private final UnitService unitService;

    @GetMapping
    public ResponseEntity<List<UnitDto>> getAllUnits() {
        return ResponseEntity.ok(unitService.getAllUnits());
    }

    @PostMapping
    public ResponseEntity<UnitDto> createUnit(@RequestBody UnitDto dto) {
        return new ResponseEntity<>(unitService.createUnit(dto), HttpStatus.CREATED);
    }

    // YENİ ENDPOINT
    @PutMapping("/{id}")
    public ResponseEntity<UnitDto> updateUnit(@PathVariable Long id, @RequestBody UnitDto dto) {
        return ResponseEntity.ok(unitService.updateUnit(id, dto));
    }

    // YENİ ENDPOINT
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUnit(@PathVariable Long id) {
        unitService.deleteUnit(id);
        return ResponseEntity.noContent().build();
    }
}