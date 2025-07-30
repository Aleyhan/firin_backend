// src/main/java/com/firinyonetim/backend/dto/shipment/request/ShipmentItemRequest.java
package com.firinyonetim.backend.dto.shipment.request;

import lombok.Data;

@Data
public class ShipmentItemRequest {
    private Long productId;
    private int cratesTaken;
    private int unitsTaken;
}