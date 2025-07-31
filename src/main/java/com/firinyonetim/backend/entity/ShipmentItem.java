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

    // --- GÜN SONU SAYIMI (GÜNCELLENDİ) ---
    // Önceki alanlar kaldırıldı: cratesReturned, unitsReturned, totalUnitsReturned

    // YENİ ALANLAR: Günlük Ürünler (Satılamayıp Kalan)
    @Column(name = "daily_crates_returned")
    private Integer dailyCratesReturned;

    @Column(name = "daily_units_returned")
    private Integer dailyUnitsReturned;

    @Column(name = "total_daily_units_returned")
    private Integer totalDailyUnitsReturned;

    // YENİ ALANLAR: İade Ürünler (Önceki Günden Gelen)
    @Column(name = "return_crates_taken")
    private Integer returnCratesTaken;

    @Column(name = "return_units_taken")
    private Integer returnUnitsTaken;

    @Column(name = "total_return_units_taken")
    private Integer totalReturnUnitsTaken;
}