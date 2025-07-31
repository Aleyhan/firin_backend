// src/main/java/com/firinyonetim/backend/entity/ShipmentItem.java
package com.firinyonetim.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@Entity
@Table(name = "shipment_items")
public class ShipmentItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Başlangıç Stok
    @Column(name = "crates_taken")
    private int cratesTaken;

    @Column(name = "units_taken")
    private int unitsTaken;

    @Column(name = "total_units_taken")
    private int totalUnitsTaken;

    // Gün Sonu Sayımı
    @Column(name = "crates_returned")
    private Integer cratesReturned;

    @Column(name = "units_returned")
    private Integer unitsReturned;

    @Column(name = "total_units_returned")
    private Integer totalUnitsReturned;

    // HESAPLANAN ALANLAR KALDIRILDI
}