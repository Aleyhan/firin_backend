// src/main/java/com/firinyonetim/backend/ewaybill/dto/request/BulkEWaybillFromTemplateRequest.java
package com.firinyonetim.backend.ewaybill.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
public class BulkEWaybillFromTemplateRequest {
    @NotEmpty
    private List<Long> customerIds;

    @NotNull
    @FutureOrPresent(message = "İrsaliye tarihi geçmiş bir tarih olamaz.")
    private LocalDate issueDate;

    @NotNull
    @FutureOrPresent(message = "İrsaliye tarihi geçmiş bir tarih olamaz.")
    private LocalTime issueTime;

    @NotNull
    @FutureOrPresent
    @FutureOrPresent(message = "Sevk tarihi geçmiş bir tarih olamaz.")
    private LocalDateTime shipmentDate;
}