// src/main/java/com/firinyonetim/backend/dto/shipment/request/ShipmentCreateRequest.java
package com.firinyonetim.backend.dto.shipment.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ShipmentCreateRequest {
    @NotNull
    private Long routeId;
    private String startNotes;
    private List<ShipmentItemRequest> items;
}