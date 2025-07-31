// src/main/java/com/firinyonetim/backend/dto/shipment/request/ShipmentUpdateRequest.java
package com.firinyonetim.backend.dto.shipment.request;

import jakarta.validation.Valid;
import lombok.Data;
import java.util.List;

@Data
public class ShipmentUpdateRequest {
    private String startNotes;
    private String endNotes;
    @Valid
    private List<ShipmentItemUpdateRequest> items;
}