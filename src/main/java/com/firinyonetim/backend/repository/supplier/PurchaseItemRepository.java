package com.firinyonetim.backend.repository.supplier;

import com.firinyonetim.backend.entity.supplier.PurchaseItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchaseItemRepository extends JpaRepository<PurchaseItem, Long> {
}