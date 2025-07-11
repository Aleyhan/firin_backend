package com.firinyonetim.backend.repository;
import com.firinyonetim.backend.entity.RouteAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface RouteAssignmentRepository extends JpaRepository<RouteAssignment, Long> {
    List<RouteAssignment> findByCustomerId(Long customerId);
    List<RouteAssignment> findByRouteId(Long routeId);
    @Transactional
    void deleteByCustomerId(Long customerId);
}