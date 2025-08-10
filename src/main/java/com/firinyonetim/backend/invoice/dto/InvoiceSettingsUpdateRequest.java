package com.firinyonetim.backend.invoice.dto;

import lombok.Data;

@Data
public class InvoiceSettingsUpdateRequest {
    private String prefix;
    private String xsltCode; // YENİ ALAN
    private Boolean useCalculatedVatAmount;
    private Boolean useCalculatedTotalSummary;
    private Boolean hideDespatchMessage;
    private String paymentMeansCode;
    private String paymentChannelCode;
    private String instructionNote;
    private String payeeFinancialAccountId;
    private String payeeFinancialAccountCurrencyCode;
}