package com.firinyonetim.backend.dto.address.request;

import lombok.Data;

@Data
public class AddressRequest {
    private String details;
    private String province;
    private String district;
}