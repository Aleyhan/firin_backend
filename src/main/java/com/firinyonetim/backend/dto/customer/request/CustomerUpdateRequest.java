package com.firinyonetim.backend.dto.customer.request;

import com.firinyonetim.backend.dto.address.request.AddressUpdateRequest;
import com.firinyonetim.backend.dto.tax_info.request.TaxInfoUpdateRequest;
import com.firinyonetim.backend.entity.DayOfWeek;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;
import java.util.Set;

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
    private AddressUpdateRequest address; // YENİ

    // Müşterinin güncel vergi bilgisi
    private TaxInfoUpdateRequest taxInfo;

    private Set<DayOfWeek> workingDays;
    private Set<DayOfWeek> irsaliyeGunleri;
}