package com.firinyonetim.backend.mapper;

import com.firinyonetim.backend.dto.driver.response.DriverCustomerResponse;
import com.firinyonetim.backend.entity.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// DEĞİŞİKLİK BURADA: AddressMapper'ı uses listesine ekliyoruz
@Mapper(componentModel = "spring", uses = {DriverCustomerProductAssignmentMapper.class, AddressMapper.class})
public interface DriverCustomerMapper {

    // DEĞİŞİKLİK BURADA: Yeni @Mapping anotasyonu ekliyoruz
    @Mapping(source = "address", target = "address")
    DriverCustomerResponse toDto(Customer customer);
}