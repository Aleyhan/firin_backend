// src/main/java/com/firinyonetim/backend/dto/transaction/response/TransactionResponse.java
package com.firinyonetim.backend.dto.transaction.response;

import com.firinyonetim.backend.entity.TransactionStatus;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TransactionResponse {
    private Long id;
    private Long customerId;
    private String customerCode;
    private String customerName;
    private Long routeId;
    private String routeName;
    private LocalDateTime transactionDate;
    private String notes;
    private Long createdByUserId;
    private String createdByUsername;
    private List<TransactionItemResponse> items;
    private List<TransactionPaymentResponse> payments;

    private TransactionStatus status;
    private String rejectionReason;
    private Integer dailySequenceNumber;

    // YENİ ALANLAR
    private Long shipmentId;
    private Integer shipmentSequenceNumber;
    private Integer sequenceInShipment; // YENİ ALAN

}