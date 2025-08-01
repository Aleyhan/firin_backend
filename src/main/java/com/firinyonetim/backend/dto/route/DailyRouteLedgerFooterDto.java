// src/main/java/com/firinyonetim/backend/dto/route/DailyRouteLedgerFooterDto.java
package com.firinyonetim.backend.dto.route;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

@Data
public class DailyRouteLedgerFooterDto {
    private Long productId;
    private String productName;
    private Map<Integer, Integer> unitsTakenByShipment = new HashMap<>();
    private Map<Integer, Integer> unitsReturnedByShipment = new HashMap<>();
    private Map<Integer, Integer> unitsSoldByShipment = new HashMap<>();
    private Map<Integer, Integer> varianceByShipment = new HashMap<>();
    // DEĞİŞİKLİK: Müşteri iadeleri de artık sefer bazında tutulacak
    private Map<Integer, Integer> unitsReturnedByCustomerByShipment = new HashMap<>();
    private int totalReturnedByCustomer; // Bu alan genel toplam için kalabilir.
}