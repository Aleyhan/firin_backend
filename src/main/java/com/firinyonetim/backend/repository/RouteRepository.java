package com.firinyonetim.backend.repository;
import com.firinyonetim.backend.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface RouteRepository extends JpaRepository<Route, Long> {


    @Query("SELECT DISTINCT r FROM Route r LEFT JOIN FETCH r.assignments WHERE r.isActive = true")
    List<Route> findAllWithCustomers();

    List<Route> findByIsActiveTrue();

    // YENİ METOT: Hem aktif olanları hem de belirli bir ID'ye sahip olanı getirir (güncelleme senaryoları için).
    // Bu, pasif bir rotayı düzenlemek istediğimizde onu bulabilmemizi sağlar.
    @Query("SELECT r FROM Route r WHERE r.isActive = true OR r.id = :id")
    List<Route> findAllActiveOrById(Long id);

    boolean existsByRouteCode(String routeCode);


}