package com.firinyonetim.backend.invoice.dto.turkcell;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TurkcellInvoiceResponse {
    @JsonProperty("Id")
    private String id;

    @JsonProperty("InvoiceNumber")
    private String invoiceNumber;
}