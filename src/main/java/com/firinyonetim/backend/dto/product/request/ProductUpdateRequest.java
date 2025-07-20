package com.firinyonetim.backend.dto.product.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductUpdateRequest {

    @NotBlank(message = "Ürün adı boş olamaz.")
    private String name;

    @NotNull(message = "Ürün fiyatı boş olamaz.")
    @Positive(message = "Ürün fiyatı pozitif olmalıdır.")
    private BigDecimal basePrice;

    @NotNull(message = "KDV oranı boş olamaz.")
    @PositiveOrZero(message = "KDV oranı 0 veya daha büyük olmalıdır.")
    private Integer vatRate;

    // DEĞİŞİKLİK: String yerine ID
    private Long productGroupId;

    // DEĞİŞİKLİK: String yerine ID
    @NotNull(message = "Birim seçilmelidir.")
    private Long unitId;

    @Positive(message = "Gramaj pozitif bir değer olmalıdır.")
    private Integer grammage;
}