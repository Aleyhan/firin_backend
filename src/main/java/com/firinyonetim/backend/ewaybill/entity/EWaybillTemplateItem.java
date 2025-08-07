// src/main/java/com/firinyonetim/backend/ewaybill/entity/EWaybillTemplateItem.java
package com.firinyonetim.backend.ewaybill.entity;

import com.firinyonetim.backend.entity.Product;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "e_waybill_template_items")
public class EWaybillTemplateItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "e_waybill_template_id", nullable = false)
    private EWaybillTemplate eWaybillTemplate;

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

    @Column(nullable = false)
    private Integer vatRate;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal vatAmount;
}