package com.firinyonetim.backend.dto.supplier.request;

import com.firinyonetim.backend.dto.address.request.AddressRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SupplierRequest {
    @NotBlank
    @Size(min = 3, max = 3, message = "Tedarikçi kodu 3 haneli olmalıdır.")
    private String supplierCode;

    @NotBlank
    private String name;

    private String contactPerson;
    private String phone;
    private String email;
    private String notes;
    private AddressRequest address;

    private Boolean isActive; // Bu alan frontend'den bu isimle geliyor.
}