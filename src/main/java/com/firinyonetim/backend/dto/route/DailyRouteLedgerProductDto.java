// src/main/java/com/firinyonetim/backend/dto/route/DailyRouteLedgerProductDto.java
package com.firinyonetim.backend.dto.route;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

@Data
public class DailyRouteLedgerProductDto {
    private Long productId;
    private String productName;
    // Key: Sefer Numarası (1, 2, ...), Value: Satış Adedi
    private Map<Integer, Integer> salesByShipment = new HashMap<>();
    // DEĞİŞİKLİK: Artık iadeler de sefer bazında tutulacak
    private Map<Integer, Integer> returnsByShipment = new HashMap<>();
    private int totalReturns; // Bu alan artık kullanılmayacak ama uyumluluk için kalabilir veya silebiliriz. Şimdilik kalsın.
}