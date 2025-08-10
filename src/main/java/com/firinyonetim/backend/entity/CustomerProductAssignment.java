package com.firinyonetim.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "customer_product_assignments", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"customer_id", "product_id"})
})
public class CustomerProductAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_type", nullable = false)
    private PricingType pricingType = PricingType.VAT_EXCLUSIVE;

    @Column(name = "special_price")
    private BigDecimal specialPrice;

    // YENİ ALANLAR: Hesaplanmış nihai fiyatlar
    @Column(name = "final_price_vat_exclusive", precision = 19, scale = 4)
    private BigDecimal finalPriceVatExclusive; // KDV Hariç Nihai Fiyat

    @Column(name = "final_price_vat_included", precision = 19, scale = 4)
    private BigDecimal finalPriceVatIncluded; // KDV Dahil Nihai Fiyat
}