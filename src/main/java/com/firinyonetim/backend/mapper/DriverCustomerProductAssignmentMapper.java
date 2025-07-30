// src/main/java/com/firinyonetim/backend/mapper/DriverCustomerProductAssignmentMapper.java
package com.firinyonetim.backend.mapper;

import com.firinyonetim.backend.dto.driver.response.DriverCustomerProductAssignmentDto;
import com.firinyonetim.backend.entity.CustomerProductAssignment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DriverCustomerProductAssignmentMapper {
    @Mapping(source = "product.id", target = "productId")
    DriverCustomerProductAssignmentDto toDto(CustomerProductAssignment assignment);
}