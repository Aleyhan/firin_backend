package com.firinyonetim.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "routes")
public class Route {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // YENİ ALAN
    @Column(unique = true, nullable = false, length = 4)
    private String routeCode;

    @Column(nullable = false, unique = true)
    private String name;

    // YENİ ALAN
    @Column(columnDefinition = "TEXT") // Açıklamanın uzun olabilmesi için TEXT tipini kullanmak iyi bir pratiktir.
    private String description;

    // Bir rotanın hangi müşterileri içerdiğini görmek için
    // Bir rotanın hangi müşterileri içerdiğini görmek için
    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude // YENİ ANOTASYON
    @EqualsAndHashCode.Exclude // YENİ ANOTASYON (İyi bir pratiktir)
    private Set<RouteAssignment> assignments = new HashSet<>();
}