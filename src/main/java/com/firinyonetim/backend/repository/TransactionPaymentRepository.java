package com.firinyonetim.backend.repository;

import com.firinyonetim.backend.entity.TransactionPayment;
import org.springframework.data.jpa.repository.JpaRepository;


public interface TransactionPaymentRepository extends JpaRepository<TransactionPayment, Long> {
}
