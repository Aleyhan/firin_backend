package com.firinyonetim.backend.dto.address.request;

import lombok.Data;

@Data
public class AddressUpdateRequest {
    private Long id; // Güncellenecek adresin ID'si (yeni ise null)
    private String details;
    private String province;
    private String district;
}