package com.firinyonetim.backend.dto.customer.request;

import com.firinyonetim.backend.dto.address.request.AddressRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class CustomerCreateRequest {

    @NotBlank(message = "Müşteri adı boş olamaz.")
    private String name;

    private String phone;
    private String email;

    // Müşteriyle birlikte adreslerini de alıyoruz.
    private List<AddressRequest> addresses;
}