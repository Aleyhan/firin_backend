package com.firinyonetim.backend.ewaybill.dto.turkcell;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TurkcellApiResponse {
    // Hem create hem de status response'ları için ortak alanlar
    private String id;
    private int status;
    private String message;

    // Create response'una özel alan
    @JsonProperty("despatchAdviceNumber")
    private String despatchAdviceNumber;

    // Status response'una özel alan
    @JsonProperty("despatchNumber")
    private String despatchNumber;
}