// src/main/java/com/firinyonetim/backend/dto/report/ProductStockSummaryDto.java
package com.firinyonetim.backend.dto.report;

import lombok.Data;

@Data
public class ProductStockSummaryDto {
    private Long productId;
    private String productName;
    private int totalUnitsTaken = 0; // Toplam Yüklenen
    private int totalUnitsSold = 0; // Toplam Satılan
    private int totalUnitsReturnedByCustomer = 0; // Müşteriden Toplam İade Gelen
    private int totalUnitsReturnedToBakery = 0; // Fırına Toplam Geri Gelen (Günlük + İade)
    private int variance = 0; // Fark
}