package com.firinyonetim.backend.dto.customer.response;

import com.firinyonetim.backend.entity.PricingType;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CustomerProductAssignmentResponse {
    private Long productId;
    private String productName;
    private String productCode;
    private String unitCode;
    private String unitName;
    private PricingType pricingType;
    private BigDecimal specialPrice;
    private BigDecimal basePrice;
    private Integer vatRate;

    // DEĞİŞİKLİK: finalUnitPrice kaldırıldı, yerine iki yeni alan geldi
    private BigDecimal finalPriceVatExclusive;
    private BigDecimal finalPriceVatIncluded;
}