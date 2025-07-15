package com.firinyonetim.backend.dto.route.request;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data public class RouteUpdateRequest {
    @NotBlank
    private String name;

    private String description;

    // PLAKA ALANI (opsiyonel)
    private String plaka;

}