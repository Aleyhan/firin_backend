package com.firinyonetim.backend.invoice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EWaybillForInvoiceDto {
    private UUID id;
    // DEĞİŞİKLİK: Alan adı 'despatchNumber' olarak güncellendi
    private String despatchNumber;
    // DEĞİŞİKLİK: Alan adı 'issueDate' olarak güncellendi
    private LocalDateTime issueDate;
}