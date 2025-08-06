package com.firinyonetim.backend.dto.customer.response;

import com.firinyonetim.backend.entity.PricingType;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CustomerProductAssignmentResponse {
    private Long productId;
    private String productName;
    private String productCode; // YENİ ALAN
    private String unitCode; // YENİ ALAN
    private String unitName; // YENİ ALAN
    private PricingType pricingType;
    private BigDecimal specialPrice;
    private BigDecimal basePrice;
    private Integer vatRate;
    private BigDecimal finalUnitPrice;
}