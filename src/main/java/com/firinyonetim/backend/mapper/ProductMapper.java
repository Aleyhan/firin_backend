package com.firinyonetim.backend.mapper;

import com.firinyonetim.backend.dto.product.request.ProductCreateRequest;
import com.firinyonetim.backend.dto.product.response.ProductResponse;
import com.firinyonetim.backend.entity.Product;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    Product toProduct(ProductCreateRequest request);
    ProductResponse toProductResponse(Product product);
}