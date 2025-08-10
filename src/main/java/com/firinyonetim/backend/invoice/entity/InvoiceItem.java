package com.firinyonetim.backend.invoice.entity;

import com.firinyonetim.backend.entity.Product;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "invoice_items")
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal totalPrice;

    @Column(nullable = false)
    private Integer vatRate;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal vatAmount;

    @Column(precision = 19, scale = 4)
    private BigDecimal discountAmount;

    @Column(columnDefinition = "TEXT")
    private String description;
}