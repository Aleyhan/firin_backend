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

    private Long routeId; // YENİ ALAN: İsteğe bağlı rota ID'si

    @NotNull(message = "İrsaliye tarihi boş olamaz.")
    private LocalDate issueDate;

    @NotNull(message = "İrsaliye saati boş olamaz.")
    private LocalTime issueTime;

    @NotNull(message = "Sevk tarihi boş olamaz.")
    @FutureOrPresent(message = "Sevk tarihi geçmiş bir tarih olamaz.")
    private LocalDateTime shipmentDate;

    private String notes;

    @NotBlank(message = "Taşıyıcı adı boş olamaz.")
    private String carrierName;

    @NotBlank(message = "Taşıyıcı VKN/TCKN'si boş olamaz.")
    private String carrierVknTckn;

    // @NotEmpty(message = "İrsaliye en az bir kalem içermelidir.")
    @Valid
    private Set<EWaybillItemRequest> items;

    @Valid   // YENİ
    private EWaybillFieldConfig fieldConfig; // YENİ ALAN


    // YENİ İÇ SINIF
    @Data
    public static class EWaybillFieldConfig {
        private boolean includeProductCode = true;
        private boolean includeUnitPrice = true;
        private boolean includeVat = true;
        private boolean includeTotals = true;
    }

}