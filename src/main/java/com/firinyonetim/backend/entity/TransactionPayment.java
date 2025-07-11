package com.firinyonetim.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import lombok.EqualsAndHashCode; // YENİ IMPORT
import lombok.ToString;         // YENİ IMPORT

@Data
@Entity
@Table(name = "transaction_payments")
public class TransactionPayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType type;
}