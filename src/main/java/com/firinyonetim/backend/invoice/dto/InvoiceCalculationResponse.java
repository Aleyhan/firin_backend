package com.firinyonetim.backend.invoice.dto;

import lombok.Data;
import java.util.List;

@Data
public class InvoiceCalculationResponse {
    private List<CalculatedInvoiceItemDto> items;
    private List<String> warnings;
}