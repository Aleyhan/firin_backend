package com.firinyonetim.backend.invoice.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CalculatedInvoiceItemDto {
    private Long productId;
    private String productName;
    private int quantity;
    private BigDecimal unitPrice; // KDV Hariç
    private Integer vatRate;      // YENİ ALAN
}