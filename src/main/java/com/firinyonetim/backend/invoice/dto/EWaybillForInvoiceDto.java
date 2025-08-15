package com.firinyonetim.backend.invoice.dto;

import com.firinyonetim.backend.ewaybill.dto.response.EWaybillItemResponse; // YENİ IMPORT
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List; // YENİ IMPORT
import java.util.UUID;

@Data
@NoArgsConstructor
// AllArgsConstructor'ı kaldırıyoruz, çünkü artık manuel dolduracağız.
public class EWaybillForInvoiceDto {
    private UUID id;
    private String despatchNumber;
    private LocalDateTime issueDate;
    private String customerName;
    private List<EWaybillItemResponse> items; // YENİ ALAN
}