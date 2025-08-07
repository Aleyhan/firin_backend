// src/main/java/com/firinyonetim/backend/ewaybill/dto/request/EWaybillTemplateItemRequest.java
package com.firinyonetim.backend.ewaybill.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EWaybillTemplateItemRequest {
    @NotNull
    private Long productId;

    @NotNull
    @DecimalMin(value = "0.000001", message = "Miktar sıfırdan büyük olmalıdır.")
    private BigDecimal quantity;

    @NotNull
    @DecimalMin(value = "0.0", message = "Birim fiyat negatif olamaz.")
    private BigDecimal unitPrice;
}