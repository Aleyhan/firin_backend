package com.firinyonetim.backend.mapper.supplier;

import com.firinyonetim.backend.dto.supplier.request.InputProductRequest;
import com.firinyonetim.backend.dto.supplier.response.InputProductResponse;
import com.firinyonetim.backend.entity.supplier.InputProduct;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping; // YENİ IMPORT
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface InputProductMapper {

    InputProductResponse toInputProductResponse(InputProduct inputProduct);

    // DÜZELTME: inputProductCode alanı için mapping eklendi.
    @Mapping(target = "id", ignore = true) // Yeni oluştururken id'yi görmezden gel
    @Mapping(target = "inputProductCode", source = "inputProductCode")
    InputProduct toInputProduct(InputProductRequest request);

    // DÜZELTME: inputProductCode alanı için mapping eklendi.
    @Mapping(target = "id", ignore = true) // Güncellerken id'yi görmezden gel
    @Mapping(target = "inputProductCode", source = "inputProductCode")
    @Mapping(target = "active", source = "isActive", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateInputProductFromDto(InputProductRequest request, @MappingTarget InputProduct inputProduct);
}