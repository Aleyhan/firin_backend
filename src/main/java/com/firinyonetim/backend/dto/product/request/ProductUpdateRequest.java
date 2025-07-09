package com.firinyonetim.backend.dto.product.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductUpdateRequest {

    @NotBlank(message = "Ürün adı boş olamaz.")
    private String name;

    @NotNull(message = "Ürün fiyatı boş olamaz.")
    @Positive(message = "Ürün fiyatı pozitif olmalıdır.")
    private BigDecimal basePrice;

    @NotNull(message = "Aktiflik durumu belirtilmelidir.")
    private Boolean isActive;
}