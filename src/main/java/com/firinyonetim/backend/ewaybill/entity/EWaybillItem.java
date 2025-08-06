package com.firinyonetim.backend.ewaybill.entity;

import com.firinyonetim.backend.entity.Product;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "e_waybill_items")
public class EWaybillItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "e_waybill_id", nullable = false)
    private EWaybill eWaybill;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String productNameSnapshot;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal quantity;

    @Column(nullable = false, length = 10)
    private String unitCode;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal lineAmount;

    // DÜZELTME: Varsayılan değer eklendi
    @Column(nullable = false, columnDefinition = "integer default 0")
    private Integer vatRate;

    // DÜZELTME: Varsayılan değer eklendi
    @Column(nullable = false, precision = 18, scale = 2, columnDefinition = "numeric(18,2) default 0.00")
    private BigDecimal vatAmount;
}