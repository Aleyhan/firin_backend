package com.firinyonetim.backend.dto.route.request;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data public class RouteCreateRequest {
    @NotBlank
    private String name;
}