package com.firinyonetim.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "addresses")
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String details;
    @Column(nullable = false)
    private String province;
    @Column(nullable = false)
    private String district;
    // YENİ ALAN
    @Column(nullable = false, length = 10)
    private String zipcode;

}