package com.firinyonetim.backend.dto.transaction.request;
import lombok.Data;
import java.util.List;
@Data public class TransactionCreateRequest {
    private Long customerId;
    private String notes;
    private List<TransactionItemRequest> items;
    private List<TransactionPaymentRequest> payments;
}