package com.firinyonetim.backend.dto.transaction.request;
import com.firinyonetim.backend.entity.PaymentType;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransactionPaymentUpdateRequest {
    private Long id; // Güncellenecek ödemenin ID'si
    private BigDecimal amount;
    private PaymentType type;
}