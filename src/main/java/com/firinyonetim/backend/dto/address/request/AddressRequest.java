// src/main/java/com/firinyonetim/backend/dto/address/request/AddressRequest.java
package com.firinyonetim.backend.dto.address.request;

import jakarta.validation.constraints.NotBlank; // YENİ IMPORT
import lombok.Data;

@Data
public class AddressRequest {
    private String details;

    // --- DEĞİŞİKLİK BURADA ---
    @NotBlank(message = "İl (province) alanı boş olamaz.")
    private String province;

    @NotBlank(message = "İlçe (district) alanı boş olamaz.")
    private String district;
    // --- DEĞİŞİKLİK SONU ---

    @NotBlank(message = "Posta Kutusu alanı boş olamaz.")
    private String zipcode;
}