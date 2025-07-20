package com.firinyonetim.backend.service;

import com.firinyonetim.backend.dto.product.UnitDto;
import com.firinyonetim.backend.entity.Unit;
import com.firinyonetim.backend.exception.ResourceNotFoundException;
import com.firinyonetim.backend.mapper.UnitMapper;
import com.firinyonetim.backend.repository.ProductRepository;
import com.firinyonetim.backend.repository.UnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UnitService {
    private final UnitRepository unitRepository;
    private final ProductRepository productRepository; // YENİ
    private final UnitMapper unitMapper;

    public List<UnitDto> getAllUnits() {
        return unitRepository.findAll().stream()
                .map(unitMapper::toDto)
                .collect(Collectors.toList());
    }

    public UnitDto createUnit(UnitDto dto) {
        Unit unit = new Unit();
        unit.setName(dto.getName());
        Unit saved = unitRepository.save(unit);
        return unitMapper.toDto(saved);
    }

    // YENİ METOT
    @Transactional
    public UnitDto updateUnit(Long id, UnitDto dto) {
        Unit unit = unitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found with id: " + id));
        unit.setName(dto.getName());
        Unit updated = unitRepository.save(unit);
        return unitMapper.toDto(updated);
    }

    // YENİ METOT
    @Transactional
    public void deleteUnit(Long id) {
        if (productRepository.existsByUnitId(id)) {
            throw new IllegalStateException("Bu birim ürünler tarafından kullanıldığı için silinemez.");
        }
        unitRepository.deleteById(id);
    }
}