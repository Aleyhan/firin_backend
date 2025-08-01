// src/main/java/com/firinyonetim/backend/dto/route/DailyRouteLedgerCustomerRowDto.java
package com.firinyonetim.backend.dto.route;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class DailyRouteLedgerCustomerRowDto {
    private Long customerId;
    private String customerName;
    private String customerCode;
    private BigDecimal startOfDayBalance;
    private BigDecimal endOfDayBalance;
    private BigDecimal cashPayment;
    private BigDecimal cardPayment;
    private List<DailyRouteLedgerProductDto> productMovements;
    private BigDecimal totalSalesAmount; // YENİ ALAN
}