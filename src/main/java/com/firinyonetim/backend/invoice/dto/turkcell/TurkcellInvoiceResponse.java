package com.firinyonetim.backend.invoice.dto.turkcell;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TurkcellInvoiceResponse {
    // DEĞİŞİKLİK: @JsonProperty içindeki alan adları PascalCase olarak güncellendi.
    @JsonProperty("id")
    private String id;

    @JsonProperty("invoiceNumber")
    private String invoiceNumber;
}