// src/main/java/com/firinyonetim/backend/dto/shipment/request/ShipmentEndRequest.java
package com.firinyonetim.backend.dto.shipment.request;

import lombok.Data;
import java.util.List;

@Data
public class ShipmentEndRequest {
    private String endNotes;
    private List<ShipmentItemEndRequest> items;
}
