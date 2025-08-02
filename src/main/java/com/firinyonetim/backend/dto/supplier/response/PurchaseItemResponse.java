package com.firinyonetim.backend.dto.supplier.response;

import lombok.Data;
import java.math.BigDecimal;
@Data
public class PurchaseItemResponse {
    private Long id;
    private Long inputProductId;
    private String inputProductName;
    private String inputProductUnit;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
}
