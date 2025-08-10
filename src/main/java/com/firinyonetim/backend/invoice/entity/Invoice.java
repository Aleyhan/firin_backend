package com.firinyonetim.backend.invoice.entity;

import com.firinyonetim.backend.entity.Customer;
import com.firinyonetim.backend.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceProfileType profileType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceType type;

    @Column(nullable = false)
    private LocalDateTime issueDate;

    @Column(nullable = false)
    private String currencyCode;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(precision = 19, scale = 4)
    private BigDecimal totalAmount; // Ara Toplam

    @Column(precision = 19, scale = 4)
    private BigDecimal totalVatAmount; // Toplam KDV

    @Column(precision = 19, scale = 4)
    private BigDecimal payableAmount; // Ödenecek Tutar

    // Entegratör Bilgileri
    @Column(unique = true)
    private String turkcellApiId; // Başarılı gönderim sonrası Turkcell'den dönen ID

    @Column(unique = true)
    private String invoiceNumber; // Başarılı gönderim sonrası Turkcell'den dönen Fatura No

    private Integer turkcellStatus;

    @Column(columnDefinition = "TEXT")
    private String statusMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<InvoiceItem> items = new HashSet<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}