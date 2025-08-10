package com.firinyonetim.backend.mapper;

import com.firinyonetim.backend.dto.customer.response.CustomerProductAssignmentResponse;
import com.firinyonetim.backend.entity.CustomerProductAssignment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CustomerProductAssignmentMapper {

    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    @Mapping(source = "product.id", target = "productCode")
    @Mapping(source = "product.unit.code", target = "unitCode")
    @Mapping(source = "product.unit.name", target = "unitName")
    @Mapping(source = "product.basePrice", target = "basePrice")
    @Mapping(source = "product.vatRate", target = "vatRate")
    // YENİ MAPPING'LER: Entity'deki yeni alanları DTO'ya aktar
    @Mapping(source = "finalPriceVatExclusive", target = "finalPriceVatExclusive")
    @Mapping(source = "finalPriceVatIncluded", target = "finalPriceVatIncluded")
    CustomerProductAssignmentResponse toResponse(CustomerProductAssignment assignment);
}