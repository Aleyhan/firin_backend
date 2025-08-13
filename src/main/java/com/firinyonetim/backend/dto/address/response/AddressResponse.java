package com.firinyonetim.backend.dto.address.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AddressResponse {
    private Long id;
    private String details;
    private String province;
    private String district;
    private String zipcode;

    // YENİ ALANLAR
    // DEĞİŞİKLİK BURADA: Double -> BigDecimal
    private BigDecimal latitude;
    private BigDecimal longitude;
}