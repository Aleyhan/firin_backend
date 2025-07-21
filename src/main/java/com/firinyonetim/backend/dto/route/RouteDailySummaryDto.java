package com.firinyonetim.backend.dto.route;

import lombok.Data;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class RouteDailySummaryDto {
    private Long routeId;
    private String routeCode;
    private String routeName;
    private BigDecimal totalSales = BigDecimal.ZERO;
    private BigDecimal totalReturns = BigDecimal.ZERO;
    private BigDecimal totalCashPayment = BigDecimal.ZERO;
    private BigDecimal totalCardPayment = BigDecimal.ZERO;
    private BigDecimal netRevenue = BigDecimal.ZERO;
    private BigDecimal balanceChange = BigDecimal.ZERO;

    // YENİ ALAN: Ürün bazlı özetleri tutacak liste.
    private List<RouteProductSummaryDto> productSummaries = new ArrayList<>();
}