// src/main/java/com/firinyonetim/backend/dto/route/DailyRouteLedgerProductDto.java
package com.firinyonetim.backend.dto.route;

import lombok.Data;
import java.util.Map;

@Data
public class DailyRouteLedgerProductDto {
    private Long productId;
    private String productName;
    // Key: Sefer Numarası (1, 2, ...), Value: Satış Adedi
    private Map<Integer, Integer> salesByShipment;
    private int totalReturns;
}