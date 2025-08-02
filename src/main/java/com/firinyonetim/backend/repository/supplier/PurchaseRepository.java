package com.firinyonetim.backend.repository.supplier;

import com.firinyonetim.backend.entity.supplier.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
}