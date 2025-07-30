// src/main/java/com/firinyonetim/backend/dto/driver/response/DriverCustomerResponse.java
package com.firinyonetim.backend.dto.driver.response;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class DriverCustomerResponse {
    private Long id;
    private String customerCode;
    private String name;
    private BigDecimal currentBalanceAmount;
    private List<DriverCustomerProductAssignmentDto> productAssignments;
}