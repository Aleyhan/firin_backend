package com.firinyonetim.backend.dto.transaction.request;
import lombok.Data;
import java.util.List;

@Data
public class TransactionUpdateRequest {
    private String notes;
    private List<TransactionItemUpdateRequest> items;
    private List<TransactionPaymentUpdateRequest> payments;
}