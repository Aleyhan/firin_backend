// src/main/java/com/firinyonetim/backend/dto/shipment/request/ShipmentItemEndRequest.java
package com.firinyonetim.backend.dto.shipment.request;

import lombok.Data;

@Data
public class ShipmentItemEndRequest {
    private Long productId;

    // YENİ ALANLAR
    private int dailyCratesReturned;
    private int dailyUnitsReturned;
    private int returnCratesTaken;
    private int returnUnitsTaken;
}