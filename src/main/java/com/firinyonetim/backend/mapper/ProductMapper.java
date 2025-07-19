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

    Product toProduct(ProductCreateRequest request);

    ProductResponse toProductResponse(Product product);

    // DEĞİŞİKLİK BURADA: Kaynak ve hedef özellik adları artık aynı ("active") olduğu için,
    // manuel @Mapping anotasyonunu tamamen kaldırıyoruz.
    // MapStruct, "active" -> "active" eşleştirmesini otomatik yapacaktır.
    void updateProductFromDto(ProductUpdateRequest dto, @MappingTarget Product product);
}