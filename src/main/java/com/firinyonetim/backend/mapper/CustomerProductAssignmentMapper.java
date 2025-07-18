package com.firinyonetim.backend.mapper;

import com.firinyonetim.backend.dto.customer.response.CustomerProductAssignmentResponse;
import com.firinyonetim.backend.entity.CustomerProductAssignment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CustomerProductAssignmentMapper {

    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    @Mapping(source = "product.basePrice", target = "basePrice") // <<< YENİ MAPPING
    CustomerProductAssignmentResponse toResponse(CustomerProductAssignment assignment);
}