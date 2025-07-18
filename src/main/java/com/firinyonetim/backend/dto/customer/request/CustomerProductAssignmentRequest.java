package com.firinyonetim.backend.dto.customer.request;

import com.firinyonetim.backend.entity.PricingType;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CustomerProductAssignmentRequest {
    private Long productId; // Hangi ürünün atanacağını belirtir
    private PricingType pricingType;
    private BigDecimal specialPrice; // Bu alan null olabilir
}