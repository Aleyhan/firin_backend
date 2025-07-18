package com.firinyonetim.backend.dto.customer.response;

import com.firinyonetim.backend.entity.PricingType;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CustomerProductAssignmentResponse {
    private Long productId;
    private String productName;
    private PricingType pricingType;
    private BigDecimal specialPrice;
    private BigDecimal basePrice; // Ürünün standart liste fiyatı

}