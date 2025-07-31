// src/main/java/com/firinyonetim/backend/dto/route/RouteShipmentProductSummaryDto.java
package com.firinyonetim.backend.dto.route;

import lombok.Data;

@Data
public class RouteShipmentProductSummaryDto {
    private Long productId;
    private String productName;
    private int totalUnitsTaken; // Toplam Yüklenen
    private int totalUnitsReturned; // Toplam Kalan (Gün Sonu)
}