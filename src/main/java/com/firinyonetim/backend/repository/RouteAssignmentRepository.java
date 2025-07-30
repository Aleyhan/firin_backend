// src/main/java/com/firinyonetim/backend/repository/RouteAssignmentRepository.java
package com.firinyonetim.backend.repository;
import com.firinyonetim.backend.entity.RouteAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface RouteAssignmentRepository extends JpaRepository<RouteAssignment, Long> {
    List<RouteAssignment> findByCustomerId(Long customerId);

    List<RouteAssignment> findByRouteIdOrderByDeliveryOrderAsc(Long routeId);

    @Transactional
    void deleteByCustomerId(Long customerId);

    @Transactional
    void deleteByRouteId(Long routeId);

    @Query("SELECT ra FROM RouteAssignment ra JOIN FETCH ra.customer JOIN FETCH ra.route")
    List<RouteAssignment> findAllWithDetails();

    // YENİ METOT
    @Query("SELECT CASE WHEN COUNT(ra) > 0 THEN true ELSE false END " +
            "FROM RouteAssignment ra " +
            "WHERE ra.customer.id = :customerId AND ra.route.driver.id = :driverId AND ra.route.isActive = true")
    boolean isCustomerAssignedToDriverActiveRoutes(Long customerId, Long driverId);
}