package com.firinyonetim.backend.dto.route.response;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RouteResponse {
    private Long id;
    private String routeCode;
    private String name;
    private String description;
    private boolean isActive;
    private String plaka;
    private Long driverId;
    private String driverName;

    // YENİ ALANLAR
    private long customerCount;
    private BigDecimal totalDebt;
}