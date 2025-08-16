// src/main/java/com/firinyonetim/backend/ewaybill/entity/EWaybill.java
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

    @Column(unique = true)
    private String turkcellApiId;

    @Column(unique = true)
    private String ewaybillNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EWaybillStatus status = EWaybillStatus.DRAFT;

    private Integer turkcellStatus;

    @Column(columnDefinition = "TEXT")
    private String statusMessage;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    private String carrierName;
    private String carrierVknTckn;
    private String plateNumber;

    @Column(columnDefinition = "TEXT")
    private String deliveryAddressJson;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "eWaybill", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<EWaybillItem> items = new HashSet<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // BU ALANLARIN EKLENDİĞİNDEN EMİN OL
    @Column(name = "invoice_id")
    private UUID invoiceId;

    @Column(name = "invoice_number")
    private String invoiceNumber;

}