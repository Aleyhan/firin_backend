package com.firinyonetim.backend.dto.address.response;

import lombok.Data;

@Data
public class AddressResponse {
    private Long id;
    private String details;
    private String province;
    private String district;
    private String zipcode; // YENİ ALAN

}