// src/main/java/com/firinyonetim/backend/ewaybill/repository/EWaybillTemplateRepository.java
package com.firinyonetim.backend.ewaybill.repository;

import com.firinyonetim.backend.ewaybill.entity.EWaybillTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List; // YENİ IMPORT
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EWaybillTemplateRepository extends JpaRepository<EWaybillTemplate, UUID> {
    Optional<EWaybillTemplate> findByCustomerId(Long customerId);
    boolean existsByCustomerId(Long customerId);
    void deleteByCustomerId(Long customerId);

    // --- EKSİK OLAN METOT BURAYA EKLENDİ ---
    List<EWaybillTemplate> findByCustomerIdIn(List<Long> customerIds);
}