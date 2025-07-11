package com.firinyonetim.backend.dto.transaction.request;
import lombok.Data;
import java.util.List;
@Data public class TransactionUpdateRequest {
    // Müşteri değiştirilemez, çünkü bu bakiye hesaplarını karıştırır.
    // Gerekirse eski işlem iptal edilip yeni müşteriye yeni işlem açılır.
    private String notes;
    private List<TransactionItemUpdateRequest> items;
    private List<TransactionPaymentRequest> payments; // Ödemeler için create DTO'su yeterli olabilir.
}