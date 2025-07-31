// src/main/java/com/firinyonetim/backend/dto/route/DailyRouteLedgerResponseDto.java
package com.firinyonetim.backend.dto.route;

import lombok.Data;
import java.util.List;
import java.util.Set;

@Data
public class DailyRouteLedgerResponseDto {
    // O günkü sefer numaralarını tutar (örn: [1, 2])
    private Set<Integer> shipmentSequences;
    // O gün işlem gören benzersiz ürünleri tutar
    private List<DailyRouteLedgerProductDto> uniqueProducts;
    // Tablonun ana gövdesi
    private List<DailyRouteLedgerCustomerRowDto> customerRows;
    // Tablonun alt özeti
    private List<DailyRouteLedgerFooterDto> footerSummary;
}