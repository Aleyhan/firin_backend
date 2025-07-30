// src/main/java/com/firinyonetim/backend/dto/shipment/response/ShipmentResponse.java
package com.firinyonetim.backend.dto.shipment.response;

import com.firinyonetim.backend.entity.ShipmentStatus; // YENİ IMPORT
import lombok.Data;
import java.time.LocalDate;

@Data
public class ShipmentResponse {
    private Long id;
    private Long routeId;
    private Long driverId;
    private LocalDate shipmentDate;
    private Integer sequenceNumber;
    private ShipmentStatus status; // YENİ ALAN
}