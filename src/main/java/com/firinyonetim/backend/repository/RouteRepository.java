package com.firinyonetim.backend.repository;
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

    // YENİ METOT: Belirli bir şoföre atanmış aktif rotaları bulur
    List<Route> findByDriverIdAndIsActiveTrue(Long driverId);
}