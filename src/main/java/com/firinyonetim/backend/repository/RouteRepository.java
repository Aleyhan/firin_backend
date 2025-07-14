package com.firinyonetim.backend.repository;
import com.firinyonetim.backend.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface RouteRepository extends JpaRepository<Route, Long> {


    @Query("SELECT DISTINCT r FROM Route r LEFT JOIN FETCH r.assignments")
    List<Route> findAllWithCustomers();

    boolean existsByRouteCode(String routeCode);


}