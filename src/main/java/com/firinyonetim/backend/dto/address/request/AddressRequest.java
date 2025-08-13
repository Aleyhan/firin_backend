package com.firinyonetim.backend.dto.address.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AddressRequest {
    private String details;

    @NotBlank(message = "İl (province) alanı boş olamaz.")
    private String province;

    @NotBlank(message = "İlçe (district) alanı boş olamaz.")
    private String district;

    @NotBlank(message = "Posta Kutusu alanı boş olamaz.")
    private String zipcode;

    // DEĞİŞİKLİK BURADA: Double -> BigDecimal
    private BigDecimal latitude;
    private BigDecimal longitude;
}