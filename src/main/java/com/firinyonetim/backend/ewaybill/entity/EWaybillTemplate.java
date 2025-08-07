// src/main/java/com/firinyonetim/backend/ewaybill/entity/EWaybillTemplate.java
package com.firinyonetim.backend.ewaybill.entity;

import com.firinyonetim.backend.entity.Customer;
import com.firinyonetim.backend.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID; // YENİ IMPORT

@Data
@Entity
@Table(name = "e_waybill_templates")
public class EWaybillTemplate {

    // --- DEĞİŞİKLİK 1: ID tipi UUID oldu ---
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // --- DEĞİŞİKLİK 2: Standart OneToOne ilişki ---
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false, unique = true)
    private Customer customer;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_updated_by_user_id", nullable = false)
    private User lastUpdatedBy;

    private String carrierName;
    private String carrierVknTckn;
    private String plateNumber;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmountWithoutVat = BigDecimal.ZERO;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal totalVatAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmountWithVat = BigDecimal.ZERO;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "eWaybillTemplate", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<EWaybillTemplateItem> items = new HashSet<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public void addItem(EWaybillTemplateItem item) {
        items.add(item);
        item.setEWaybillTemplate(this);
    }

    public void clearItems() {
        this.items.clear();
    }
}