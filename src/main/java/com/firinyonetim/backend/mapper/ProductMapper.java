// src/main/java/com/firinyonetim/backend/mapper/ProductMapper.java
package com.firinyonetim.backend.mapper;

import com.firinyonetim.backend.dto.product.request.ProductCreateRequest;
import com.firinyonetim.backend.dto.product.request.ProductUpdateRequest;
import com.firinyonetim.backend.dto.product.response.ProductResponse;
import com.firinyonetim.backend.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    // Servis katmanı ID'lerden entity'leri bulacağı için mapper'da ignore ediyoruz.
    @Mapping(target = "productGroup", ignore = true)
    @Mapping(target = "unit", ignore = true)
    @Mapping(source = "unitsPerCrate", target = "unitsPerCrate") // YENİ MAPPING
    Product toProduct(ProductCreateRequest request);

    // Entity'den DTO'ya dönüşüm
    @Mapping(source = "productGroup.id", target = "productGroupId")
    @Mapping(source = "productGroup.name", target = "productGroupName")
    @Mapping(source = "unit.id", target = "unitId")
    @Mapping(source = "unit.name", target = "unitName")
    @Mapping(source = "unitsPerCrate", target = "unitsPerCrate") // YENİ MAPPING
    ProductResponse toProductResponse(Product product);

    // Servis katmanı ID'lerden entity'leri bulacağı için mapper'da ignore ediyoruz.
    @Mapping(target = "productGroup", ignore = true)
    @Mapping(target = "unit", ignore = true)
    @Mapping(source = "unitsPerCrate", target = "unitsPerCrate") // YENİ MAPPING
    void updateProductFromDto(ProductUpdateRequest dto, @MappingTarget Product product);
}