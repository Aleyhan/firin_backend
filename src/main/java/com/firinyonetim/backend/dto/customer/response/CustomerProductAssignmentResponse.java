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
    private BigDecimal basePrice;
    private Integer vatRate; // KDV hesaplaması için bu alana da ihtiyacımız var

    // YENİ ALAN: Backend'de hesaplanıp frontend'e hazır gönderilecek nihai birim fiyat.
    private BigDecimal finalUnitPrice;
}