package com.firinyonetim.backend.dto.route.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
@Data public class RouteCreateRequest {

    @NotBlank
    @Size(min = 4, max = 4, message = "Rota kodu tam olarak 4 haneli olmalıdır.")
    private String routeCode;


    @NotBlank
    private String name;

    private String description;


}