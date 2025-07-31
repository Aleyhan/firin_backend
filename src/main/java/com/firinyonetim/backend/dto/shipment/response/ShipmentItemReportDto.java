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

    // --- GÜN SONU (GÜNCELLENDİ) ---
    // Önceki alanlar kaldırıldı: cratesReturned, unitsReturned, totalUnitsReturned

    // YENİ ALANLAR: Günlük Kalan
    private Integer dailyCratesReturned;
    private Integer dailyUnitsReturned;
    private Integer totalDailyUnitsReturned;

    // YENİ ALANLAR: İade Gelen
    private Integer returnCratesTaken;
    private Integer returnUnitsTaken;
    private Integer totalReturnUnitsTaken;

    // Anlık Hesaplanacak Değerler
    private Integer totalUnitsSold;
    private Integer totalUnitsReturnedByCustomer;
    private Integer expectedUnitsInVehicle;
    private Integer variance;
}