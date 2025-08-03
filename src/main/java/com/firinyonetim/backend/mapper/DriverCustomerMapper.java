// src/main/java/com/firinyonetim/backend/mapper/DriverCustomerMapper.java
package com.firinyonetim.backend.mapper;

import com.firinyonetim.backend.dto.driver.response.DriverCustomerResponse;
import com.firinyonetim.backend.entity.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {DriverCustomerProductAssignmentMapper.class})
public interface DriverCustomerMapper {
    DriverCustomerResponse toDto(Customer customer);
}