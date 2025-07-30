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

    // YENİ ALANLAR: Gün Sonu Sayımı
    @Column(name = "crates_returned")
    private Integer cratesReturned; // Araçta kalan kasa

    @Column(name = "units_returned")
    private Integer unitsReturned; // Araçta kalan adet

    @Column(name = "total_units_returned")
    private Integer totalUnitsReturned; // Araçta kalan toplam adet

    // YENİ ALANLAR: Raporlama için hesaplanacak değerler
    @Column(name = "total_units_sold")
    private Integer totalUnitsSold; // Gün içi toplam satış

    @Column(name = "total_units_returned_by_customer")
    private Integer totalUnitsReturnedByCustomer; // Müşteriden iade alınan

    @Column(name = "expected_units_in_vehicle")
    private Integer expectedUnitsInVehicle; // Araçta olması beklenen

    @Column(name = "variance")
    private Integer variance; // Fark (Beklenen - Gerçekleşen)
}