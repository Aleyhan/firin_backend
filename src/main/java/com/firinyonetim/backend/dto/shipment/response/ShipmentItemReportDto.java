// src/main/java/com/firinyonetim/backend/dto/shipment/response/ShipmentItemReportDto.java
package com.firinyonetim.backend.dto.shipment.response;

import lombok.Data;

@Data
public class ShipmentItemReportDto {
    private Long productId;
    private String productName;

    // Başlangıç
    private int cratesTaken;
    private int unitsTaken;
    private int totalUnitsTaken;

    // Gün Sonu
    private Integer cratesReturned;
    private Integer unitsReturned;
    private Integer totalUnitsReturned;

    // Hesaplanan Değerler
    private Integer totalUnitsSold;
    private Integer totalUnitsReturnedByCustomer;
    private Integer expectedUnitsInVehicle;
    private Integer variance;
}