// src/main/java/com/firinyonetim/backend/dto/dashboard/DailyShipmentSummaryDto.java
package com.firinyonetim.backend.dto.dashboard;

import lombok.Data;
import java.util.List;

@Data
public class DailyShipmentSummaryDto {
    private int totalShipments; // O günkü toplam sefer sayısı
    private List<ProductShipmentSummaryDto> productSummaries;
}