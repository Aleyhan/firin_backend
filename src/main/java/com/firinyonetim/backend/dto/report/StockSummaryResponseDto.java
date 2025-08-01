// src/main/java/com/firinyonetim/backend/dto/report/StockSummaryResponseDto.java
package com.firinyonetim.backend.dto.report;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class StockSummaryResponseDto {
    // Tüm seferlerin kümülatif ürün özeti
    private List<ProductStockSummaryDto> cumulativeSummary = new ArrayList<>();
    // Rota bazlı gruplanmış özet
    private List<RouteStockSummaryDto> routeBasedSummary = new ArrayList<>();
}