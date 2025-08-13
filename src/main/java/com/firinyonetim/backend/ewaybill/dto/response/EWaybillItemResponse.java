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
    // YENİ ALANLAR
    private BigDecimal priceVatExclusive;
    private BigDecimal priceVatIncluded;
    private Integer vatRate;

}