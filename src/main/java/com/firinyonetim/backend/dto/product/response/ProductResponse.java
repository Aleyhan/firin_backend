// src/main/java/com/firinyonetim/backend/dto/product/response/ProductResponse.java
package com.firinyonetim.backend.dto.product.response;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductResponse {
    private Long id;
    private String name;
    private BigDecimal basePrice;
    private Integer vatRate;
    private Long productGroupId;
    private String productGroupName;
    private Long unitId;
    private String unitName;
    private Integer grammage;

    // YENİ ALAN
    private Integer unitsPerCrate;
}