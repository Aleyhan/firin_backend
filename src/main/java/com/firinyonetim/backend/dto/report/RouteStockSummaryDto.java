// src/main/java/com/firinyonetim/backend/dto/report/RouteStockSummaryDto.java
package com.firinyonetim.backend.dto.report;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RouteStockSummaryDto {
    private Long routeId;
    private String routeName;
    private List<ProductStockSummaryDto> cumulativeProductSummaries = new ArrayList<>();
    private List<ShipmentStockSummaryDto> shipmentSummaries = new ArrayList<>();
}