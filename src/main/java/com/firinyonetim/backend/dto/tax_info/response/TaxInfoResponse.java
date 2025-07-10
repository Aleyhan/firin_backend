package com.firinyonetim.backend.dto.tax_info.response;

import lombok.Data;

@Data
public class TaxInfoResponse {
    private String tradeName;
    private String taxNumber;
    private String taxOffice;
}