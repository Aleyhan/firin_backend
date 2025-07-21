package com.firinyonetim.backend.dto.transaction.request;
import lombok.Data;
import java.time.LocalDate; // LocalDate import'u eklendi
import java.util.List;

@Data
public class TransactionCreateRequest {
    private Long customerId;
    private Long routeId;
    private String notes;
    private List<TransactionItemRequest> items;
    private List<TransactionPaymentRequest> payments;

    // YENİ ALAN: Frontend'den gelecek olan işlem tarihi.
    private LocalDate transactionDate;
}