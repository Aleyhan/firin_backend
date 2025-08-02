package com.firinyonetim.backend.dto.supplier.response;

import com.firinyonetim.backend.entity.PaymentType;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PurchasePaymentResponse {
    private Long id;
    private Long supplierId;
    private String supplierName;
    private LocalDateTime paymentDate;
    private BigDecimal amount;
    private PaymentType type;
    private String notes;
    private String createdByUsername;
}