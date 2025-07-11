package com.firinyonetim.backend.dto.transaction.response;

import com.firinyonetim.backend.entity.PaymentType;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransactionPaymentResponse {
    private Long id;
    private BigDecimal amount;
    private PaymentType type;
}