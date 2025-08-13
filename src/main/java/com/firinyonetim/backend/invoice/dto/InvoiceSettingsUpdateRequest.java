package com.firinyonetim.backend.invoice.dto;

import lombok.Data;
import java.util.List;

@Data
public class InvoiceSettingsUpdateRequest {
    private String prefix;
    private String xsltCode;
    private Boolean useCalculatedVatAmount;
    private Boolean useCalculatedTotalSummary;
    private Boolean hideDespatchMessage;
    private String paymentMeansCode;
    private String paymentChannelCode;
    private String instructionNote;
    private String payeeFinancialAccountId;
    private String payeeFinancialAccountCurrencyCode;
    private List<String> defaultNotes; // YENİ ALAN
}