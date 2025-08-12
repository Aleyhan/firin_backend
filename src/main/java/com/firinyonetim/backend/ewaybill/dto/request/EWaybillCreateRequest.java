// src/main/java/com/firinyonetim/backend/ewaybill/dto/request/EWaybillCreateRequest.java
package com.firinyonetim.backend.ewaybill.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

@Data
public class EWaybillCreateRequest {
    @NotNull(message = "Müşteri ID'si boş olamaz.")
    private Long customerId;

    private Long routeId;

    @NotNull(message = "İrsaliye tarihi boş olamaz.")
    @FutureOrPresent(message = "İrsaliye tarihi geçmiş bir tarih olamaz.")
    private LocalDate issueDate;

    @NotNull(message = "İrsaliye saati boş olamaz.")
    @FutureOrPresent(message = "İrsaliye tarihi geçmiş bir tarih olamaz.")
    private LocalTime issueTime;

    @NotNull(message = "Sevk tarihi boş olamaz.")
    private LocalDateTime shipmentDate;

    private String notes;

    @NotBlank(message = "Taşıyıcı adı boş olamaz.")
    private String carrierName;

    @NotBlank(message = "Taşıyıcı VKN/TCKN'si boş olamaz.")
    private String carrierVknTckn;

    @NotEmpty(message = "İrsaliye en az bir kalem içermelidir.")
    @Valid
    private Set<EWaybillItemRequest> items;
}