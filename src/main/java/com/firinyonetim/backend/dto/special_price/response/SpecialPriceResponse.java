package com.firinyonetim.backend.dto.special_price.response;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SpecialPriceResponse {
    private Long productId;
    private String productName;
    private BigDecimal price;
}