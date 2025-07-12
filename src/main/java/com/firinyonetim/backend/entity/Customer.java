package com.firinyonetim.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
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

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Address> addresses = new ArrayList<>();

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SpecialProductPrice> specialPrices = new ArrayList<>();

    @OneToOne(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private TaxInfo taxInfo;
}