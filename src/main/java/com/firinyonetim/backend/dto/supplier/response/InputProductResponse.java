package com.firinyonetim.backend.dto.supplier.response;

import lombok.Data;

@Data
public class InputProductResponse {
    private Long id;
    private String inputProductCode; // YENİ ALAN
    private String name;
    private String unit;
    private String description;
    private boolean isActive;
}