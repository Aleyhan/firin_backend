package com.firinyonetim.backend.invoice.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class InvoiceItemResponse {
    private Long id;
    private Long productId;
    private String productNameSnapshot;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private Integer vatRate;
    private BigDecimal vatAmount;
    private BigDecimal discountAmount;
    private String description;
}