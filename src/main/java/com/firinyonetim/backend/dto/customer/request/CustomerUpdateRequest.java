package com.firinyonetim.backend.dto.customer.request;

import com.firinyonetim.backend.dto.address.request.AddressUpdateRequest;
import com.firinyonetim.backend.dto.tax_info.request.TaxInfoUpdateRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class CustomerUpdateRequest {
    @NotBlank
    private String name;
    private String notes; // Yeni alan

    private String phone;
    private String email;
    @NotNull
    private Boolean isActive;

    // Müşterinin güncel adres listesi
    private List<AddressUpdateRequest> addresses;

    // Müşterinin güncel vergi bilgisi
    private TaxInfoUpdateRequest taxInfo;
}