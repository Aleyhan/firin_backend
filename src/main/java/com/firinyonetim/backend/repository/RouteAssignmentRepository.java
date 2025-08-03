// src/main/java/com/firinyonetim/backend/repository/RouteAssignmentRepository.java
package com.firinyonetim.backend.repository;
import com.firinyonetim.backend.entity.RouteAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface RouteAssignmentRepository extends JpaRepository<RouteAssignment, Long> {
    List<RouteAssignment> findByCustomerId(Long customerId);

    // --- BU SATIR GÜNCELLENDİ ---
    // Müşteriyi (c) çekerken, ona ait çalışma günlerini (workingDays) de açıkça çekmesi için LEFT JOIN FETCH c.workingDays eklendi.
    // DISTINCT, birden fazla koleksiyon çekildiğinde oluşabilecek satır tekrarlarını engeller.
    @Query("SELECT DISTINCT ra FROM RouteAssignment ra JOIN FETCH ra.customer c LEFT JOIN FETCH c.workingDays WHERE ra.route.id = :routeId ORDER BY ra.deliveryOrder ASC, c.customerCode ASC")
    List<RouteAssignment> findByRouteIdOrderByDeliveryOrderAsc(@Param("routeId") Long routeId);

    @Transactional
    void deleteByCustomerId(Long customerId);

    @Transactional
    void deleteByRouteId(Long routeId);

    @Query("SELECT ra FROM RouteAssignment ra JOIN FETCH ra.customer JOIN FETCH ra.route")
    List<RouteAssignment> findAllWithDetails();

    @Query("SELECT CASE WHEN COUNT(ra) > 0 THEN true ELSE false END " +
            "FROM RouteAssignment ra " +
            "WHERE ra.customer.id = :customerId AND ra.route.driver.id = :driverId AND ra.route.isActive = true")
    boolean isCustomerAssignedToDriverActiveRoutes(@Param("customerId") Long customerId, @Param("driverId") Long driverId);
}