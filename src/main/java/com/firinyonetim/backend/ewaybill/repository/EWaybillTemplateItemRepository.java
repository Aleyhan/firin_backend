// src/main/java/com/firinyonetim/backend/ewaybill/repository/EWaybillTemplateItemRepository.java
package com.firinyonetim.backend.ewaybill.repository;

import com.firinyonetim.backend.ewaybill.entity.EWaybillTemplateItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EWaybillTemplateItemRepository extends JpaRepository<EWaybillTemplateItem, Long> {
}