// src/main/java/com/firinyonetim/backend/ewaybill/dto/response/TransactionProductSummaryDto.java
package com.firinyonetim.backend.ewaybill.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionProductSummaryDto {
    private Long productId;
    private String productName;
    private String unitCode;
    private BigDecimal totalQuantity;
}