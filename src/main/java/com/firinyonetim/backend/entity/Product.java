package com.firinyonetim.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private BigDecimal basePrice; // kdv dahil fiyatı

    @Column(nullable = false)
    private Integer vatRate;

    // DEĞİŞİKLİK: String yerine ProductGroup entity'sine referans
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_group_id")
    private ProductGroup productGroup;

    // DEĞİŞİKLİK: String yerine Unit entity'sine referans
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    private Unit unit;

    private Integer grammage;
}