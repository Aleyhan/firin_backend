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

    // ProductCreateRequest'ten Product'a dönüşüm.
    // Alan isimleri aynı olduğu için özel bir @Mapping'e gerek yok.
    // MapStruct tüm alanları (name, basePrice, vatRate, productGroup, unit, grammage) otomatik olarak map'leyecektir.
    Product toProduct(ProductCreateRequest request);

    // Product'tan ProductResponse'a dönüşüm.
    ProductResponse toProductResponse(Product product);

    // YENİ EKLENDİ: Update işlemi için özel bir metot.
    // Bu metot, var olan bir product nesnesini, gelen request DTO'sundaki verilerle günceller.
    // @MappingTarget anotasyonu, 'product' parametresinin güncellenecek hedef olduğunu belirtir.
    void updateProductFromDto(ProductUpdateRequest dto, @MappingTarget Product product);
}