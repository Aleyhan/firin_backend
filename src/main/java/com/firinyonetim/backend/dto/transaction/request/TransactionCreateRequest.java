// src/main/java/com/firinyonetim/backend/dto/transaction/request/TransactionCreateRequest.java
package com.firinyonetim.backend.dto.transaction.request;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class TransactionCreateRequest {
    @NotNull
    private Long customerId;
    private Long routeId;
    private String notes;
    private List<TransactionItemRequest> items;
    private List<TransactionPaymentRequest> payments;
    private LocalDate transactionDate;

    private Long shipmentId;
}