// src/main/java/com/firinyonetim/backend/entity/Product.java
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_group_id")
    private ProductGroup productGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    private Unit unit;

    private Integer grammage;

    // YENİ ALAN
    @Column(name = "units_per_crate")
    private Integer unitsPerCrate;
}