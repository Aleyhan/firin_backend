// src/main/java/com/firinyonetim/backend/dto/route/RouteShipmentSummaryDto.java
package com.firinyonetim.backend.dto.route;

import lombok.Data;
import java.util.List;

@Data
public class RouteShipmentSummaryDto {
    private int totalShipments; // O günkü toplam sefer sayısı
    private List<RouteShipmentProductSummaryDto> productSummaries;
}