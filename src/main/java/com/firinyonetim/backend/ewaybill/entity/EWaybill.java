package com.firinyonetim.backend.ewaybill.entity;

import com.firinyonetim.backend.entity.Customer;
import com.firinyonetim.backend.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@Entity
@Table(name = "e_waybills")
public class EWaybill {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Turkcell API Entegrasyon Bilgileri
    @Column(unique = true)
    private String turkcellApiId; // Turkcell'den dönen UUID

    @Column(unique = true)
    private String ewaybillNumber; // D03... ile başlayan resmi numara

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EWaybillStatus status = EWaybillStatus.DRAFT;

    private Integer turkcellStatus; // 0, 20, 40, 60, 70 gibi

    @Column(columnDefinition = "TEXT")
    private String statusMessage; // API'den dönen mesaj

    // Genel İrsaliye Bilgileri
    @Column(nullable = false)
    private LocalDate issueDate;

    @Column(nullable = false)
    private LocalTime issueTime;

    @Column(nullable = false)
    private LocalDateTime shipmentDate;

    @Column(length = 3, nullable = false)
    private String currencyCode = "TRY";

    @Column(columnDefinition = "TEXT")
    private String notes;

    // İlişkili Taraflar
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer; // Alıcı

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    // Taşıyıcı Bilgileri (Şoför veya Firma olabilir)
    private String carrierName; // Taşıyıcı Unvanı veya Şoför Adı Soyadı
    private String carrierVknTckn; // Taşıyıcı VKN/TCKN

    // Adres Bilgileri (JSON olarak saklanabilir veya ayrı bir entity yapılabilir)
    @Column(columnDefinition = "TEXT")
    private String deliveryAddressJson;

    // Kalemler
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "eWaybill", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<EWaybillItem> items = new HashSet<>();

    // Zaman Damgaları
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}