package com.firinyonetim.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "units")
public class Unit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    // YENİ ALAN: GİB/UBL standardındaki birim kodu (C62, KGM vb.)
    @Column(nullable = true, unique = true, length = 10)
    private String code;
}