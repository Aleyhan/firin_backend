package com.firinyonetim.backend.entity.supplier;

import com.firinyonetim.backend.entity.Address;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "suppliers")
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 3)
    private String supplierCode;

    @Column(nullable = false)
    private String name;

    @Column(length = 100)
    private String contactPerson;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(nullable = false)
    private BigDecimal currentBalanceAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean isActive = true; // DEĞİŞİKLİK BURADA

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "address_id", referencedColumnName = "id")
    private Address address;

    @OneToOne(mappedBy = "supplier", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private SupplierTaxInfo taxInfo;
}