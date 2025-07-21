package com.firinyonetim.backend.dto.dashboard;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class DashboardDailySummaryDto {
    private BigDecimal totalSales = BigDecimal.ZERO;
    private BigDecimal totalReturns = BigDecimal.ZERO;
    private BigDecimal totalCashPayment = BigDecimal.ZERO;
    private BigDecimal totalCardPayment = BigDecimal.ZERO;
    private BigDecimal netRevenue = BigDecimal.ZERO;
    private BigDecimal balanceChange = BigDecimal.ZERO;
}