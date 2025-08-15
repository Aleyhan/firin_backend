package com.firinyonetim.backend.invoice.entity;

public enum InvoiceStatus {
    // İç Durumlar
    DRAFT,                 // Taslak - Henüz gönderilmedi
    API_ERROR,             // API Hatası - Gönderim sırasında anlık hata alındı

    // Turkcell / GİB Durumları
    SENDING,               // Kuyrukta veya GİB'e iletiliyor (20, 30, 50)
    AWAITING_APPROVAL,     // Onay Bekliyor (Ticari Fatura) (70)
    APPROVING,             // Onaylanıyor (61)
    REJECTING,             // Reddediliyor (81)

    // Sonuç Durumları
    APPROVED,              // Onaylandı (60, 65)
    REJECTED_BY_GIB,       // Hata / GİB Tarafından Reddedildi (40, 62, 82)
    REJECTED_BY_RECIPIENT, // Alıcı Tarafından Reddedildi (80)
    CANCELLED              // İptal Edildi (99)
}