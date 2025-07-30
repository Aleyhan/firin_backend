// src/main/java/com/firinyonetim/backend/repository/ShipmentRepository.java
package com.firinyonetim.backend.repository;

import com.firinyonetim.backend.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Optional;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    @Query("SELECT COUNT(s) FROM Shipment s WHERE s.route.id = :routeId AND s.shipmentDate = :date")
    int countByRouteIdAndShipmentDate(Long routeId, LocalDate date);

    // YENİ METOT: Belirli bir rota ve tarih için en son seferi bulur.
    Optional<Shipment> findTopByRouteIdAndShipmentDateOrderBySequenceNumberDesc(Long routeId, LocalDate date);
}