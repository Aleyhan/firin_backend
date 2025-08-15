package com.firinyonetim.backend.dto.route.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull; // YENİ IMPORT
import lombok.Data;
@Data
public class RouteUpdateRequest {
    @NotBlank
    private String name;

    private String description;

    private String plaka;

    private Long driverId;

    // YENİ ALAN
    @NotNull(message = "Aktiflik durumu boş olamaz.")
    private Boolean isActive;
}