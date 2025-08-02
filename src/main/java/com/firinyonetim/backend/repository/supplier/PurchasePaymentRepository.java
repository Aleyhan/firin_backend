package com.firinyonetim.backend.repository.supplier;

import com.firinyonetim.backend.entity.supplier.PurchasePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchasePaymentRepository extends JpaRepository<PurchasePayment, Long> {
}