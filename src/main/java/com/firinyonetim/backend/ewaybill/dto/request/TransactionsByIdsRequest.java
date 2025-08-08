// src/main/java/com/firinyonetim/backend/ewaybill/dto/request/TransactionsByIdsRequest.java
package com.firinyonetim.backend.ewaybill.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class TransactionsByIdsRequest {
    @NotEmpty
    private List<Long> transactionIds;
}