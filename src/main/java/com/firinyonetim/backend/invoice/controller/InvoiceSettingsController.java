package com.firinyonetim.backend.invoice.controller;

import com.firinyonetim.backend.invoice.dto.InvoiceSettingsDto;
import com.firinyonetim.backend.invoice.dto.InvoiceSettingsUpdateRequest;
import com.firinyonetim.backend.invoice.service.InvoiceSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invoice-settings")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('YONETICI', 'DEVELOPER', 'MUHASEBE')")
public class InvoiceSettingsController {

    private final InvoiceSettingsService invoiceSettingsService;

    @GetMapping
    public ResponseEntity<InvoiceSettingsDto> getInvoiceSettings() {
        return ResponseEntity.ok(invoiceSettingsService.getInvoiceSettings());
    }

    @PutMapping
    public ResponseEntity<InvoiceSettingsDto> updateInvoiceSettings(@Valid @RequestBody InvoiceSettingsUpdateRequest request) {
        return ResponseEntity.ok(invoiceSettingsService.updateInvoiceSettings(request));
    }
}