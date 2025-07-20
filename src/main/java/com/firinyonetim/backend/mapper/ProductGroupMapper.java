package com.firinyonetim.backend.mapper;

import com.firinyonetim.backend.dto.product.ProductGroupDto;
import com.firinyonetim.backend.entity.ProductGroup;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductGroupMapper {
    ProductGroupDto toDto(ProductGroup productGroup);
}