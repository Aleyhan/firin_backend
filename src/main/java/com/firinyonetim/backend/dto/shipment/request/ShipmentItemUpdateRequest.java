// src/main/java/com/firinyonetim/backend/dto/shipment/request/ShipmentItemUpdateRequest.java
package com.firinyonetim.backend.dto.shipment.request;

import lombok.Data;

@Data
public class ShipmentItemUpdateRequest {
    private Long productId;
    // Başlangıç
    private int cratesTaken;
    private int unitsTaken;
    // Bitiş
    private Integer dailyCratesReturned;
    private Integer dailyUnitsReturned;
    private Integer returnCratesTaken;
    private Integer returnUnitsTaken;
}