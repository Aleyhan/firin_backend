package com.firinyonetim.backend.ewaybill.entity;

public enum EWaybillStatus {
    DRAFT,              // Taslak - Kullanıcı tarafından oluşturuldu, henüz gönderilmedi
    SENDING,            // Gönderiliyor - Turkcell API'sine gönderildi, GİB onayı bekleniyor
    AWAITING_APPROVAL,  // Onay Bekliyor - GİB'den geçti, alıcının onayı bekleniyor (Turkcell status: 70)
    APPROVED,           // Onaylandı - Alıcı tarafından kabul edildi (Turkcell status: 60)
    REJECTED_BY_GIB,    // GİB Tarafından Reddedildi - Güncellenemez, iptal edilmeli (Turkcell status: 40)
    REJECTED_BY_RECIPIENT, // Alıcı Tarafından Reddedildi
    CANCELLED,          // İptal Edildi - Kullanıcı tarafından veya GİB hatası sonrası iptal edildi
    API_ERROR           // API Hatası - Turkcell'e gönderilirken anlık hata alındı (Turkcell status: 40 öncesi)
}