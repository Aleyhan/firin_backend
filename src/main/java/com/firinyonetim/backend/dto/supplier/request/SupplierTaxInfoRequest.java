package com.firinyonetim.backend.dto.supplier.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SupplierTaxInfoRequest {
    @NotBlank
    private String tradeName;
    @NotBlank
    private String taxNumber;
    @NotBlank
    private String taxOffice;
}