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
    private BigDecimal basePrice; // Bu fiyat KDV hariç mi, dahil mi? Karar verilmeli.

    // YENİ EKLENEN ALANLAR
    @Column(nullable = false)
    private Integer vatRate; // Vergi Oranı (KDV), örn: 1, 10, 20 gibi tam sayı

    private String productGroup; // Ürün Grubu, örn: "Ekmekler", "Pastalar"

    private String unit; // Birim, örn: "Adet", "Kg", "Paket"

    private Integer grammage; // Gramaj, örn: 250, 1000


}