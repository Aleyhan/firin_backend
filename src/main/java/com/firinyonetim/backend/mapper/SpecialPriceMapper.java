package com.firinyonetim.backend.mapper;

import com.firinyonetim.backend.dto.special_price.response.SpecialPriceResponse;
import com.firinyonetim.backend.entity.SpecialProductPrice;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SpecialPriceMapper {
    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    SpecialPriceResponse toSpecialPriceResponse(SpecialProductPrice specialPrice);
}