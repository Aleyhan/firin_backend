package com.firinyonetim.backend.mapper;

import com.firinyonetim.backend.dto.customer.response.CustomerProductAssignmentResponse;
import com.firinyonetim.backend.entity.CustomerProductAssignment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CustomerProductAssignmentMapper {

    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    @Mapping(source = "product.id", target = "productCode") // YENİ MAPPING (ID'yi koda mapliyoruz)
    @Mapping(source = "product.unit.code", target = "unitCode") // YENİ MAPPING
    @Mapping(source = "product.unit.name", target = "unitName") // YENİ MAPPING

    @Mapping(source = "product.basePrice", target = "basePrice")
    @Mapping(source = "product.vatRate", target = "vatRate") // YENİ MAPPING
    CustomerProductAssignmentResponse toResponse(CustomerProductAssignment assignment);
}