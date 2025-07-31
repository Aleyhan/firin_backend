// src/main/java/com/firinyonetim/backend/dto/dashboard/ProductShipmentSummaryDto.java
package com.firinyonetim.backend.dto.dashboard;

import lombok.Data;

@Data
public class ProductShipmentSummaryDto {
    private Long productId;
    private String productName;
    private int totalUnitsTaken; // Toplam Yüklenen
    private int totalUnitsReturned; // Toplam Geri Gelen
    private int netDispatch; // Net Çıkış
}