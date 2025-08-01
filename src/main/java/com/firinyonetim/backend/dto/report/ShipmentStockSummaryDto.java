// src/main/java/com/firinyonetim/backend/dto/report/ShipmentStockSummaryDto.java
package com.firinyonetim.backend.dto.report;

import com.firinyonetim.backend.entity.ShipmentStatus;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ShipmentStockSummaryDto {
    private Long shipmentId;
    private Integer sequenceNumber;
    private String driverName;
    private ShipmentStatus status;
    private List<ProductStockSummaryDto> productSummaries = new ArrayList<>();
}