package com.firinyonetim.backend.invoice.dto;

import com.firinyonetim.backend.invoice.entity.InvoiceProfileType;
import com.firinyonetim.backend.invoice.entity.InvoiceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID; // YENİ IMPORT

@Data
public class InvoiceCreateRequest {
    @NotNull
    private Long customerId;

    @NotNull
    private InvoiceProfileType profileType;

    @NotNull
    private InvoiceType type;

    @NotNull
    private LocalDateTime issueDate;

    @NotNull
    private String currencyCode;

    private String notes;

    @NotEmpty
    @Valid
    private List<InvoiceItemRequest> items;

    // YENİ ALAN: Faturaya bağlanacak irsaliye ID'leri
    private List<UUID> relatedEWaybillIds;
}