package com.firinyonetim.backend.dto.product.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AffectedCustomerDto {
    private Long customerId;
    private String customerCode;
    private String customerName;
    private BigDecimal specialPrice;
}