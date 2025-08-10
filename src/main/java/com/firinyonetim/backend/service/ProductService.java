package com.firinyonetim.backend.service;

import com.firinyonetim.backend.dto.product.request.ProductCreateRequest;
import com.firinyonetim.backend.dto.product.request.ProductUpdateRequest;
import com.firinyonetim.backend.dto.product.response.AffectedCustomerDto;
import com.firinyonetim.backend.dto.product.response.ProductResponse;
import com.firinyonetim.backend.entity.CustomerProductAssignment;
import com.firinyonetim.backend.entity.Product;
import com.firinyonetim.backend.entity.ProductGroup;
import com.firinyonetim.backend.entity.Unit;
import com.firinyonetim.backend.exception.ResourceNotFoundException;
import com.firinyonetim.backend.mapper.ProductMapper;
import com.firinyonetim.backend.repository.CustomerProductAssignmentRepository;
import com.firinyonetim.backend.repository.ProductGroupRepository;
import com.firinyonetim.backend.repository.ProductRepository;
import com.firinyonetim.backend.repository.UnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    // DEĞİŞİKLİK: Yeni repository'ler eklendi
    private final ProductGroupRepository productGroupRepository;
    private final UnitRepository unitRepository;
    private final CustomerProductAssignmentRepository customerProductAssignmentRepository;

    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request) {
        Product product = productMapper.toProduct(request);

        // ID'den Unit entity'sini bul ve set et
        Unit unit = unitRepository.findById(request.getUnitId())
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found with id: " + request.getUnitId()));
        product.setUnit(unit);

        // ID'den ProductGroup entity'sini bul ve set et (eğer ID varsa)
        if (request.getProductGroupId() != null) {
            ProductGroup productGroup = productGroupRepository.findById(request.getProductGroupId())
                    .orElseThrow(() -> new ResourceNotFoundException("ProductGroup not found with id: " + request.getProductGroupId()));
            product.setProductGroup(productGroup);
        }

        Product savedProduct = productRepository.save(product);
        return productMapper.toProductResponse(savedProduct);
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(productMapper::toProductResponse)
                .collect(Collectors.toList());
    }

    public ProductResponse getProductById(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
        return productMapper.toProductResponse(product);
    }

    @Transactional
    public ProductResponse updateProduct(Long productId, ProductUpdateRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        productMapper.updateProductFromDto(request, product);

        // ID'den Unit entity'sini bul ve set et
        Unit unit = unitRepository.findById(request.getUnitId())
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found with id: " + request.getUnitId()));
        product.setUnit(unit);

        // ID'den ProductGroup entity'sini bul ve set et (eğer ID varsa)
        if (request.getProductGroupId() != null) {
            ProductGroup productGroup = productGroupRepository.findById(request.getProductGroupId())
                    .orElseThrow(() -> new ResourceNotFoundException("ProductGroup not found with id: " + request.getProductGroupId()));
            product.setProductGroup(productGroup);
        } else {
            product.setProductGroup(null); // Eğer grup kaldırıldıysa null set et
        }

        Product savedProduct = productRepository.save(product);
        return productMapper.toProductResponse(savedProduct);
    }

    @Transactional
    public void deleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        productRepository.delete(product);
    }
    // YENİ METOT
    @Transactional(readOnly = true)
    public List<AffectedCustomerDto> getCustomersWithSpecialPriceForProduct(Long productId) {
        List<CustomerProductAssignment> assignments = customerProductAssignmentRepository.findByProductIdAndSpecialPriceIsNotNull(productId);

        return assignments.stream().map(assignment -> {
            AffectedCustomerDto dto = new AffectedCustomerDto();
            dto.setCustomerId(assignment.getCustomer().getId());
            dto.setCustomerCode(assignment.getCustomer().getCustomerCode());
            dto.setCustomerName(assignment.getCustomer().getName());
            dto.setSpecialPrice(assignment.getSpecialPrice());
            return dto;
        }).collect(Collectors.toList());
    }


}


