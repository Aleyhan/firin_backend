package com.firinyonetim.backend.invoice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "invoice_settings")
public class InvoiceSettings {

    @Id
    private Long id;

    // generalInfoModel
    @Column(nullable = true)
    private String prefix;

    // YENİ ALAN
    @Column(nullable = true)
    private String xsltCode;

    // ublSettingsModel
    @Column(nullable = true)
    private Boolean useCalculatedVatAmount;

    @Column(nullable = true)
    private Boolean useCalculatedTotalSummary;

    @Column(nullable = true)
    private Boolean hideDespatchMessage;

    // paymentMeansModel (Firma Banka Bilgileri vb.)
    @Column(nullable = true)
    private String paymentMeansCode;

    @Column(nullable = true)
    private String paymentChannelCode;

    @Column(nullable = true)
    private String instructionNote;

    @Column(nullable = true)
    private String payeeFinancialAccountId; // IBAN vb.

    @Column(nullable = true)
    private String payeeFinancialAccountCurrencyCode; // IBAN Para Birimi
}