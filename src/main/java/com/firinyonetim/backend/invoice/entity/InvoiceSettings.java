package com.firinyonetim.backend.invoice.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "invoice_settings")
public class InvoiceSettings {

    @Id
    private Long id;

    @Column(nullable = true)
    private String prefix;

    @Column(nullable = true)
    private String xsltCode;

    @Column(nullable = true)
    private Boolean useCalculatedVatAmount;

    @Column(nullable = true)
    private Boolean useCalculatedTotalSummary;

    @Column(nullable = true)
    private Boolean hideDespatchMessage;

    @Column(nullable = true)
    private String paymentMeansCode;

    @Column(nullable = true)
    private String paymentChannelCode;

    @Column(nullable = true)
    private String instructionNote;

    @Column(nullable = true)
    private String payeeFinancialAccountId;

    @Column(nullable = true)
    private String payeeFinancialAccountCurrencyCode;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "invoice_settings_notes", joinColumns = @JoinColumn(name = "settings_id"))
    @Column(name = "note", columnDefinition = "TEXT")
    // DEĞİŞİKLİK BURADA: Liste doğrudan initialize ediliyor.
    private List<String> defaultNotes = new ArrayList<>();
}