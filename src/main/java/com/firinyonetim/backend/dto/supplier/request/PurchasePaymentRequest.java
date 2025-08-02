package com.firinyonetim.backend.dto.supplier.request;

import com.firinyonetim.backend.entity.PaymentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PurchasePaymentRequest {
    @NotNull
    private Long supplierId;

    private LocalDateTime paymentDate;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    private PaymentType type;

    private String notes;
}

