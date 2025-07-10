package com.firinyonetim.backend.dto.tax_info.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TaxInfoUpdateRequest {
    @NotBlank
    private String tradeName;
    @NotBlank
    private String taxNumber;
    @NotBlank
    private String taxOffice;
}