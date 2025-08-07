// src/main/java/com/firinyonetim/backend/ewaybill/dto/response/EWaybillTemplateItemResponse.java
package com.firinyonetim.backend.ewaybill.dto.response;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class EWaybillTemplateItemResponse {
    private Long id;
    private Long productId;
    private String productNameSnapshot;
    private BigDecimal quantity;
    private String unitCode;
}