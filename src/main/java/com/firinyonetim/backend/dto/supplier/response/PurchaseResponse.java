package com.firinyonetim.backend.dto.supplier.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PurchaseResponse {
    private Long id;
    private Long supplierId;
    private String supplierName;
    private LocalDateTime purchaseDate;
    private String invoiceNumber;
    private BigDecimal totalAmount;
    private String notes;
    private String createdByUsername;
    private List<PurchaseItemResponse> items;
}