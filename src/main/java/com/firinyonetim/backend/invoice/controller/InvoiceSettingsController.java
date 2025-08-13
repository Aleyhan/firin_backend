package com.firinyonetim.backend.invoice.controller;

import com.firinyonetim.backend.invoice.dto.InvoiceSettingsDto;
import com.firinyonetim.backend.invoice.dto.InvoiceSettingsUpdateRequest;
import com.firinyonetim.backend.invoice.service.InvoiceSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional; // YENİ IMPORT

// ----- BU IMPORT'LARI EKLE -----
import com.firinyonetim.backend.invoice.entity.InvoiceSettings;
import com.firinyonetim.backend.invoice.repository.InvoiceSettingsRepository;
import org.hibernate.Hibernate;


@RestController
@RequestMapping("/api/invoice-settings")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('YONETICI', 'DEVELOPER', 'MUHASEBE')")
public class InvoiceSettingsController {

    private final InvoiceSettingsService invoiceSettingsService;
    private final InvoiceSettingsRepository invoiceSettingsRepository; // YENİ REPOSITORY

    @GetMapping
    public ResponseEntity<InvoiceSettingsDto> getInvoiceSettings() {
        return ResponseEntity.ok(invoiceSettingsService.getInvoiceSettings());
    }

    @PutMapping
    public ResponseEntity<InvoiceSettingsDto> updateInvoiceSettings(@Valid @RequestBody InvoiceSettingsUpdateRequest request) {
        return ResponseEntity.ok(invoiceSettingsService.updateInvoiceSettings(request));
    }

    // ----- YENİ TEST ENDPOINT'İ -----
    @GetMapping("/test")
    @PreAuthorize("permitAll") // DEĞİŞİKLİK BURADA: Sınıf seviyesindeki kuralı ezer.
    @Transactional(readOnly = true) // Lazy loading için transaction'ı açık tutar
    public ResponseEntity<InvoiceSettings> getSettingsForTest() {
        InvoiceSettings settings = invoiceSettingsRepository.findById(1L).orElse(null);

        if (settings == null) {
            return ResponseEntity.notFound().build();
        }

        // Koleksiyonun yüklenmesini zorla
        Hibernate.initialize(settings.getDefaultNotes());

        return ResponseEntity.ok(settings);
    }
}