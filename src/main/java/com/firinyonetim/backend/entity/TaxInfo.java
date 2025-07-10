package com.firinyonetim.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "tax_infos")
public class TaxInfo {
    @Id
    private Long id; // Customer ID'yi Primary Key olarak kullanacağız (One-to-One)

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId // Bu anotasyon, id alanının customer ilişkisinden alınacağını belirtir.
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(nullable = false)
    private String tradeName; // Ticari Unvan

    @Column(nullable = false, unique = true)
    private String taxNumber; // Vergi Numarası

    @Column(nullable = false)
    private String taxOffice; // Vergi Dairesi
}