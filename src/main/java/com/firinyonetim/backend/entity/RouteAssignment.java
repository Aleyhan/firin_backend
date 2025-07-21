package com.firinyonetim.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@Entity
@Table(name = "route_assignments", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"route_id", "customer_id"})
})
public class RouteAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Customer customer;

    // YENİ ALAN: Teslimat sırası
    @Column(name = "delivery_order")
    private Integer deliveryOrder;
    // YENİ ALAN SONU
}