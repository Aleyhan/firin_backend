package com.firinyonetim.backend.dto.supplier.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InputProductRequest {
    @NotBlank
    @Size(min = 5, max = 5, message = "Ürün kodu 5 haneli olmalıdır.") // YENİ ALAN
    private String inputProductCode;

    @NotBlank(message = "Ürün adı boş olamaz.")
    private String name;

    @NotBlank(message = "Birim boş olamaz.")
    private String unit;

    private String description;

    private Boolean isActive; // YENİ ALAN
}