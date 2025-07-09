package com.firinyonetim.backend.dto.product.response;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductResponse {
    private Long id;
    private String name;
    private BigDecimal basePrice;
    private boolean isActive;
}