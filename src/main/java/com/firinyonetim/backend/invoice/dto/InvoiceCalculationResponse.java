package com.firinyonetim.backend.invoice.dto;

import lombok.Data;
import java.util.List;

@Data
public class InvoiceCalculationResponse {
    private List<CalculatedInvoiceItemDto> items;
    // DEĞİŞİKLİK: 'warnings' yerine iki yeni liste
    private List<String> errors;
    private List<String> priceWarnings;
}