// src/main/java/com/firinyonetim/backend/invoice/dto/InvoiceResponse.java
package com.firinyonetim.backend.invoice.dto;

import com.firinyonetim.backend.invoice.entity.InvoiceProfileType;
import com.firinyonetim.backend.invoice.entity.InvoiceStatus;
import com.firinyonetim.backend.invoice.entity.InvoiceType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
public class InvoiceResponse {
    private UUID id;
    private Long customerId;
    private String customerName;
    private String customerCode;
    private InvoiceStatus status;
    private InvoiceProfileType profileType;
    private InvoiceType type;
    private LocalDateTime issueDate;
    private String currencyCode;
    private String notes;
    private BigDecimal totalAmount;
    private BigDecimal totalVatAmount;
    private BigDecimal payableAmount;
    private String turkcellApiId;
    private String invoiceNumber;
    private Integer turkcellStatus;
    private String statusMessage;
    private String createdByUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Set<InvoiceItemResponse> items;

    // YENİ EKLENEN ALAN
    private String relatedDespatchesJson;
}