// src/main/java/com/firinyonetim/backend/mapper/DriverCustomerMapper.java
package com.firinyonetim.backend.mapper;

import com.firinyonetim.backend.dto.driver.response.DriverCustomerResponse;
import com.firinyonetim.backend.entity.Customer;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {DriverCustomerProductAssignmentMapper.class})
public interface DriverCustomerMapper {
    DriverCustomerResponse toDto(Customer customer);
}