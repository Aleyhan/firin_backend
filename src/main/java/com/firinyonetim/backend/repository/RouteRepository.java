// src/main/java/com/firinyonetim/backend/repository/RouteRepository.java
package com.firinyonetim.backend.repository;
import com.firinyonetim.backend.dto.route.RouteSummaryDto;
import com.firinyonetim.backend.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface RouteRepository extends JpaRepository<Route, Long> {


    @Query("SELECT DISTINCT r FROM Route r LEFT JOIN FETCH r.assignments WHERE r.isActive = true")
    List<Route> findAllWithCustomers();

    List<Route> findByIsActiveTrue();

    @Query("SELECT r FROM Route r WHERE r.isActive = true OR r.id = :id")
    List<Route> findAllActiveOrById(Long id);

    boolean existsByRouteCode(String routeCode);

    List<Route> findByDriverIdAndIsActiveTrue(Long driverId);

    // YENİ METOT: Rotaları, müşteri sayıları ve toplam borçları ile birlikte tek sorguda getirir.
    @Query("SELECT new com.firinyonetim.backend.dto.route.RouteSummaryDto(r, COUNT(ra.customer.id), COALESCE(SUM(ra.customer.currentBalanceAmount), 0.0)) " +
            "FROM Route r " +
            "LEFT JOIN r.assignments ra " +
            "GROUP BY r.id, r.routeCode, r.name, r.description, r.isActive, r.plaka, r.driver")
    List<RouteSummaryDto> findAllWithSummary();
}