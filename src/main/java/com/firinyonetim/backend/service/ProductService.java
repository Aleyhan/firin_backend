package com.firinyonetim.backend.service;

import com.firinyonetim.backend.dto.product.request.ProductCreateRequest;
import com.firinyonetim.backend.dto.product.response.ProductResponse;
import com.firinyonetim.backend.entity.Product;
import com.firinyonetim.backend.mapper.ProductMapper;
import com.firinyonetim.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductResponse createProduct(ProductCreateRequest request) {
        Product product = productMapper.toProduct(request);
        return productMapper.toProductResponse(productRepository.save(product));
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(productMapper::toProductResponse)
                .collect(Collectors.toList());
    }
}