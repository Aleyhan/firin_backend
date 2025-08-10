package com.firinyonetim.backend.invoice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InvoiceItemRequest {
    @NotNull
    private Long productId;

    @NotNull
    @Positive
    private int quantity;

    @NotNull
    @Positive
    private BigDecimal unitPrice;

    private BigDecimal discountAmount;

    private String description;
}