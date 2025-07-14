package com.firinyonetim.backend.dto.route.response;
import lombok.Data;
@Data public class RouteResponse {
    private Long id;
    private String routeCode; // YENİ ALAN

    private String name;
    private String description;

    // YENİ ALAN
    private boolean isActive;


}