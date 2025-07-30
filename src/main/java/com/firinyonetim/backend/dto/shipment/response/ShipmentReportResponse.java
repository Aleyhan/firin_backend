// src/main/java/com/firinyonetim/backend/dto/shipment/response/ShipmentReportResponse.java
package com.firinyonetim.backend.dto.shipment.response;

import com.firinyonetim.backend.entity.ShipmentStatus;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ShipmentReportResponse {
    private Long id;
    private String routeName;
    private String driverName;
    private LocalDate shipmentDate;
    private Integer sequenceNumber;
    private ShipmentStatus status;
    private String startNotes;
    private String endNotes;
    private List<ShipmentItemReportDto> items;
}