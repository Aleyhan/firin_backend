package com.firinyonetim.backend.service;

import com.firinyonetim.backend.dto.product.ProductGroupDto;
import com.firinyonetim.backend.entity.ProductGroup;
import com.firinyonetim.backend.exception.ResourceNotFoundException;
import com.firinyonetim.backend.mapper.ProductGroupMapper;
import com.firinyonetim.backend.repository.ProductGroupRepository;
import com.firinyonetim.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductGroupService {
    private final ProductGroupRepository productGroupRepository;
    private final ProductRepository productRepository; // YENİ
    private final ProductGroupMapper productGroupMapper;

    public List<ProductGroupDto> getAllProductGroups() {
        return productGroupRepository.findAll().stream()
                .map(productGroupMapper::toDto)
                .collect(Collectors.toList());
    }

    public ProductGroupDto createProductGroup(ProductGroupDto dto) {
        ProductGroup productGroup = new ProductGroup();
        productGroup.setName(dto.getName());
        ProductGroup saved = productGroupRepository.save(productGroup);
        return productGroupMapper.toDto(saved);
    }

    // YENİ METOT
    @Transactional
    public ProductGroupDto updateProductGroup(Long id, ProductGroupDto dto) {
        ProductGroup productGroup = productGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProductGroup not found with id: " + id));
        productGroup.setName(dto.getName());
        ProductGroup updated = productGroupRepository.save(productGroup);
        return productGroupMapper.toDto(updated);
    }

    // YENİ METOT
    @Transactional
    public void deleteProductGroup(Long id) {
        if (productRepository.existsByProductGroupId(id)) {
            throw new IllegalStateException("Bu ürün grubu ürünler tarafından kullanıldığı için silinemez.");
        }
        productGroupRepository.deleteById(id);
    }
}