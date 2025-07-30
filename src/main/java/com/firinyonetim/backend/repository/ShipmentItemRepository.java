// src/main/java/com/firinyonetim/backend/repository/ShipmentItemRepository.java
package com.firinyonetim.backend.repository;

import com.firinyonetim.backend.entity.ShipmentItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentItemRepository extends JpaRepository<ShipmentItem, Long> {
}