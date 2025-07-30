// src/main/java/com/firinyonetim/backend/repository/ShipmentRepository.java
package com.firinyonetim.backend.repository;

import com.firinyonetim.backend.entity.Shipment;
import com.firinyonetim.backend.entity.ShipmentStatus; // YENİ
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List; // YENİ
import java.util.Optional;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    @Query("SELECT COUNT(s) FROM Shipment s WHERE s.route.id = :routeId AND s.shipmentDate = :date")
    int countByRouteIdAndShipmentDate(Long routeId, LocalDate date);

    Optional<Shipment> findTopByRouteIdAndShipmentDateOrderBySequenceNumberDesc(Long routeId, LocalDate date);

    // YENİ METOT
    @Query("SELECT s FROM Shipment s JOIN FETCH s.route JOIN FETCH s.driver WHERE s.status = :status ORDER BY s.shipmentDate DESC, s.route.name, s.sequenceNumber")
    List<Shipment> findByStatusWithDetails(ShipmentStatus status);

    // YENİ METOT
    @Query("SELECT s FROM Shipment s JOIN FETCH s.route JOIN FETCH s.driver LEFT JOIN FETCH s.items i LEFT JOIN FETCH i.product WHERE s.id = :id")
    Optional<Shipment> findByIdWithDetails(Long id);
}