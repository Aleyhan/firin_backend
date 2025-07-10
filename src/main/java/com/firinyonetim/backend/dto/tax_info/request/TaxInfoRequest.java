package com.firinyonetim.backend.dto.tax_info.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TaxInfoRequest {

    @NotBlank(message = "Ticari unvan boş olamaz.")
    private String tradeName;

    @NotBlank(message = "Vergi numarası boş olamaz.")
    private String taxNumber;

    @NotBlank(message = "Vergi dairesi boş olamaz.")
    private String taxOffice;
}