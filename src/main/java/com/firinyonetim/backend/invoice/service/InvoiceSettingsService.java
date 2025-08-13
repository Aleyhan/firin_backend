package com.firinyonetim.backend.invoice.service;

import com.firinyonetim.backend.invoice.dto.InvoiceSettingsDto;
import com.firinyonetim.backend.invoice.dto.InvoiceSettingsUpdateRequest;
import com.firinyonetim.backend.invoice.entity.InvoiceSettings;
import com.firinyonetim.backend.invoice.mapper.InvoiceSettingsMapper;
import com.firinyonetim.backend.invoice.repository.InvoiceSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class InvoiceSettingsService {

    private final InvoiceSettingsRepository invoiceSettingsRepository;
    private final InvoiceSettingsMapper invoiceSettingsMapper; // Mapper'ı hala diğer alanlar için kullanıyoruz
    private static final Long SETTINGS_ID = 1L;

    @Transactional
    public InvoiceSettingsDto getInvoiceSettings() {
        InvoiceSettings settings = invoiceSettingsRepository.findById(SETTINGS_ID)
                .orElseGet(() -> {
                    InvoiceSettings newSettings = new InvoiceSettings();
                    newSettings.setId(SETTINGS_ID);
                    return invoiceSettingsRepository.save(newSettings);
                });

        // Manuel olarak DTO oluşturuyoruz. Bu en garantili yöntemdir.
        InvoiceSettingsDto dto = new InvoiceSettingsDto();
        dto.setId(settings.getId());
        dto.setPrefix(settings.getPrefix());
        dto.setXsltCode(settings.getXsltCode());
        dto.setUseCalculatedVatAmount(settings.getUseCalculatedVatAmount());
        dto.setUseCalculatedTotalSummary(settings.getUseCalculatedTotalSummary());
        dto.setHideDespatchMessage(settings.getHideDespatchMessage());
        // ... diğer basit alanlar ...
        dto.setPaymentMeansCode(settings.getPaymentMeansCode());
        dto.setPaymentChannelCode(settings.getPaymentChannelCode());
        dto.setInstructionNote(settings.getInstructionNote());
        dto.setPayeeFinancialAccountId(settings.getPayeeFinancialAccountId());
        dto.setPayeeFinancialAccountCurrencyCode(settings.getPayeeFinancialAccountCurrencyCode());

        // Koleksiyonun null olup olmadığını kontrol edip DTO'ya atıyoruz.
        if (settings.getDefaultNotes() != null) {
            // Lazy loading'e karşı koleksiyonu initialize et
            Hibernate.initialize(settings.getDefaultNotes());
            dto.setDefaultNotes(new ArrayList<>(settings.getDefaultNotes()));
        } else {
            // Eğer null ise, DTO'ya boş bir liste koy.
            dto.setDefaultNotes(new ArrayList<>());
        }

        return dto;
    }

    @Transactional(readOnly = true)
    public InvoiceSettings getSettings() {
        InvoiceSettings settings = invoiceSettingsRepository.findById(SETTINGS_ID)
                .orElseThrow(() -> new IllegalStateException("Fatura ayarları bulunamadı. Lütfen önce ayarları kaydedin."));
        if (settings.getDefaultNotes() == null) {
            settings.setDefaultNotes(new ArrayList<>());
        } else {
            Hibernate.initialize(settings.getDefaultNotes());
        }
        return settings;
    }

    @Transactional
    public InvoiceSettingsDto updateInvoiceSettings(InvoiceSettingsUpdateRequest request) {
        InvoiceSettings settings = invoiceSettingsRepository.findById(SETTINGS_ID)
                .orElseThrow(() -> new IllegalStateException("Güncellenecek fatura ayarları bulunamadı."));

        invoiceSettingsMapper.updateFromDto(request, settings);

        // Bu mantık doğru ve burada kalmalı.
        if (settings.getDefaultNotes() == null) {
            settings.setDefaultNotes(new ArrayList<>());
        }

        settings.getDefaultNotes().clear();
        if (request.getDefaultNotes() != null) {
            settings.getDefaultNotes().addAll(request.getDefaultNotes());
        }

        InvoiceSettings updatedSettings = invoiceSettingsRepository.save(settings);

        // Güncellenmiş entity'yi tekrar DTO'ya çevirip döndür.
        return getInvoiceSettings();
    }
}