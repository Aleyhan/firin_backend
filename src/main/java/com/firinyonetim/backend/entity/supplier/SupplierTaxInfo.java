package com.firinyonetim.backend.entity.supplier;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "supplier_tax_infos")
public class SupplierTaxInfo {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(nullable = false)
    private String tradeName;

    @Column(nullable = false, unique = true)
    private String taxNumber;

    @Column(nullable = false)
    private String taxOffice;
}