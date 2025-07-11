package com.firinyonetim.backend.dto.transaction.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TransactionResponse {
    private Long id;
    private Long customerId;
    private String customerName;
    private LocalDateTime transactionDate;
    private String notes;
    private Long createdByUserId;
    private String createdByUsername;
    private List<TransactionItemResponse> items;
    private List<TransactionPaymentResponse> payments;
}