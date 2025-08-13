package com.firinyonetim.backend.invoice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
// @AllArgsConstructor ANOTASYONUNU SİLİN VEYA YORUM SATIRI YAPIN
public class EWaybillForInvoiceDto {
    private UUID id;
    private String despatchNumber;
    private LocalDateTime issueDate;
    private String customerName;
}