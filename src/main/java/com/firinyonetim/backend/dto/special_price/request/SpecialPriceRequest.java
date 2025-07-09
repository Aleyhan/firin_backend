package com.firinyonetim.backend.dto.special_price.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class SpecialPriceRequest {

    @NotNull(message = "Ürün ID'si boş olamaz.")
    private Long productId;

    @NotNull(message = "Fiyat boş olamaz.")
    @Positive(message = "Fiyat pozitif olmalıdır.")
    private BigDecimal price;
}