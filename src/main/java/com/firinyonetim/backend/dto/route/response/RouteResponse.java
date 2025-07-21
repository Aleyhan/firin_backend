package com.firinyonetim.backend.dto.route.response;
import lombok.Data;
@Data public class RouteResponse {
    private Long id;
    private String routeCode;

    private String name;
    private String description;

    private boolean isActive;

    private String plaka;

    // YENİ ALANLAR
    private Long driverId;
    private String driverName;
}