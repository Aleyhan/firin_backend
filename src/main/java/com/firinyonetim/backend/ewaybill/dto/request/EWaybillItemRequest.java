// src/main/java/com/firinyonetim/backend/ewaybill/dto/request/EWaybillItemRequest.java
package com.firinyonetim.backend.ewaybill.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EWaybillItemRequest {
    @NotNull(message = "Ürün ID'si boş olamaz.")
    private Long productId;

    @NotNull(message = "Miktar boş olamaz.")
    @DecimalMin(value = "0.000001", message = "Miktar sıfırdan büyük olmalıdır.")
    private BigDecimal quantity;
}