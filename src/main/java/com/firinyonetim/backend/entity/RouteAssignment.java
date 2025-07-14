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
    @ToString.Exclude // YENİ ANOTASYON
    @EqualsAndHashCode.Exclude // YENİ ANOTASYON
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    @ToString.Exclude // YENİ ANOTASYON
    @EqualsAndHashCode.Exclude // YENİ ANOTASYON
    private Customer customer;
}