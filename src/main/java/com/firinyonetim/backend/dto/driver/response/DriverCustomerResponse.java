package com.firinyonetim.backend.dto.driver.response;

import com.firinyonetim.backend.dto.address.response.AddressResponse; // YENİ IMPORT
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class DriverCustomerResponse {
    private Long id;
    private String customerCode;
    private String name;
    private BigDecimal currentBalanceAmount;
    private boolean isActive;
    private List<DriverCustomerProductAssignmentDto> productAssignments;
    private List<String> workingDays;
    private AddressResponse address; // YENİ ALAN
}