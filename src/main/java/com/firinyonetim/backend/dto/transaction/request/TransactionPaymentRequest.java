package com.firinyonetim.backend.dto.transaction.request;
import com.firinyonetim.backend.entity.PaymentType;
import lombok.Data;
import java.math.BigDecimal;
@Data public class TransactionPaymentRequest {
    private BigDecimal amount;
    private PaymentType type; // "NAKIT" veya "KART"
}