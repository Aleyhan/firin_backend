package com.firinyonetim.backend.invoice.entity;

public enum InvoiceStatus {
    DRAFT,              // Taslak - Henüz entegratöre gönderilmedi
    SENDING,            // Gönderiliyor - Turkcell API'sine gönderildi, GİB onayı bekleniyor
    APPROVED,           // Onaylandı
    REJECTED_BY_GIB,    // GİB Tarafından Reddedildi
    API_ERROR,          // API Hatası - Gönderim sırasında anlık hata alındı
    CANCELLED           // İptal Edildi
}