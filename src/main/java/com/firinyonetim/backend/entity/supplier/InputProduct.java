package com.firinyonetim.backend.entity.supplier;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "input_products")
public class InputProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 5)
    private String inputProductCode;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String unit;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private boolean isActive = true; // DEĞİŞİKLİK BURADA
}