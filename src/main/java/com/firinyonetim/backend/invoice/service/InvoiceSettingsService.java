package com.firinyonetim.backend.invoice.service;

import com.firinyonetim.backend.invoice.dto.InvoiceSettingsDto;
import com.firinyonetim.backend.invoice.dto.InvoiceSettingsUpdateRequest;
import com.firinyonetim.backend.invoice.entity.InvoiceSettings;
import com.firinyonetim.backend.invoice.mapper.InvoiceSettingsMapper;
import com.firinyonetim.backend.invoice.repository.InvoiceSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvoiceSettingsService {

    private final InvoiceSettingsRepository invoiceSettingsRepository;
    private final InvoiceSettingsMapper invoiceSettingsMapper;
    private static final Long SETTINGS_ID = 1L;

    @Transactional
    public InvoiceSettingsDto getInvoiceSettings() {
        InvoiceSettings settings = invoiceSettingsRepository.findById(SETTINGS_ID)
                .orElseGet(() -> {
                    InvoiceSettings newSettings = new InvoiceSettings();
                    newSettings.setId(SETTINGS_ID);
                    return invoiceSettingsRepository.save(newSettings);
                });
        return invoiceSettingsMapper.toDto(settings);
    }

    @Transactional(readOnly = true)
    public InvoiceSettings getSettings() {
        return invoiceSettingsRepository.findById(SETTINGS_ID)
                .orElseThrow(() -> new IllegalStateException("Fatura ayarları bulunamadı. Lütfen önce ayarları kaydedin."));
    }

    @Transactional
    public InvoiceSettingsDto updateInvoiceSettings(InvoiceSettingsUpdateRequest request) {
        InvoiceSettings settings = invoiceSettingsRepository.findById(SETTINGS_ID)
                .orElseThrow(() -> new IllegalStateException("Güncellenecek fatura ayarları bulunamadı."));

        invoiceSettingsMapper.updateFromDto(request, settings);
        InvoiceSettings updatedSettings = invoiceSettingsRepository.save(settings);
        return invoiceSettingsMapper.toDto(updatedSettings);
    }
}