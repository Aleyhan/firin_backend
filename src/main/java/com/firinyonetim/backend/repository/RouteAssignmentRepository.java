package com.firinyonetim.backend.repository;
import com.firinyonetim.backend.entity.RouteAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface RouteAssignmentRepository extends JpaRepository<RouteAssignment, Long> {
    List<RouteAssignment> findByCustomerId(Long customerId);
    List<RouteAssignment> findByRouteId(Long routeId);
    @Transactional
    void deleteByCustomerId(Long customerId);

    @Transactional
    void deleteByRouteId(Long routeId);

    // YENİ METOT: Tüm atamaları müşteri ve rota bilgileriyle birlikte getirir.
    @Query("SELECT ra FROM RouteAssignment ra JOIN FETCH ra.customer JOIN FETCH ra.route")
    List<RouteAssignment> findAllWithDetails();
}