// src/main/java/com/firinyonetim/backend/dto/route/DailyRouteLedgerFooterDto.java
package com.firinyonetim.backend.dto.route;

import lombok.Data;
import java.util.Map;

@Data
public class DailyRouteLedgerFooterDto {
    private Long productId;
    private String productName;
    // Key: Sefer Numarası (1, 2, ...), Value: Adet
    private Map<Integer, Integer> unitsTakenByShipment;
    private Map<Integer, Integer> unitsReturnedByShipment;
    private Map<Integer, Integer> unitsSoldByShipment; // YENİ
    private Map<Integer, Integer> varianceByShipment; // YENİ
    private int totalReturnedByCustomer;
}