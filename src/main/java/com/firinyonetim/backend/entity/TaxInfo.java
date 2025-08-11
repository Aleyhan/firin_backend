package com.firinyonetim.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Data
@Entity
@Table(name = "tax_infos")
@ToString(exclude = "customer") // DEĞİŞİKLİK BURADA
public class TaxInfo {
    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(nullable = false)
    private String tradeName;

    @Column(nullable = false, unique = true)
    private String taxNumber;

    @Column(nullable = false)
    private String taxOffice;
}