package com.firinyonetim.backend.invoice.dto.turkcell;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TurkcellInvoiceStatusResponse {
    @JsonProperty("id")
    private String id;

    @JsonProperty("invoiceNumber")
    private String invoiceNumber;

    @JsonProperty("status")
    private int status;

    @JsonProperty("message")
    private String message;

    @JsonProperty("envelopeId")
    private String envelopeId;

    @JsonProperty("envelopeStatus")
    private int envelopeStatus;

    @JsonProperty("envelopeMessage")
    private String envelopeMessage;
}