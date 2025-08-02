package com.firinyonetim.backend.dto.supplier.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PurchaseRequest {
    @NotNull
    private Long supplierId;

    private LocalDateTime purchaseDate;
    private String invoiceNumber;
    private String notes;

    @NotEmpty
    @Valid
    private List<PurchaseItemRequest> items;
}