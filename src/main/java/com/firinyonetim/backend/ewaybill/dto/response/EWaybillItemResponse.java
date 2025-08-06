package com.firinyonetim.backend.ewaybill.dto.response;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class EWaybillItemResponse {
    private Long id;
    private Long productId;
    private String productNameSnapshot;
    private BigDecimal quantity;
    private String unitCode;
    private BigDecimal unitPrice;
    private BigDecimal lineAmount;
    private Integer vatRate; // YENİ
    private BigDecimal vatAmount; // YENİ
}