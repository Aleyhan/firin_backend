package com.firinyonetim.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.util.Set;
import java.util.HashSet;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "customers")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // YENİ EKLENDİ: Benzersiz, 4 haneli müşteri kodu
    @Column(unique = true, nullable = false, length = 4)
    private String customerCode;

    @Column(nullable = false)
    private String name;

    // YENİ EKLENDİ: Müşteriyle ilgili kalıcı notlar
    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private BigDecimal currentBalanceAmount = BigDecimal.ZERO;

    private String phone;
    private String email;
    private boolean isActive = true;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "address_id", referencedColumnName = "id") // YENİ: Müşteri tablosuna adresin ID'sini ekliyoruz.
    private Address address; // List<Address> yerine tek bir Address nesnesi

    @OneToOne(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private TaxInfo taxInfo;

    @ElementCollection(targetClass = DayOfWeek.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "customer_working_days", joinColumns = @JoinColumn(name = "customer_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private Set<DayOfWeek> workingDays = new HashSet<>();
}