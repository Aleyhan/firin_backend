package com.firinyonetim.backend.repository;

import com.firinyonetim.backend.entity.TransactionPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TransactionPaymentRepository extends JpaRepository<TransactionPayment, Long> {
    @Query("SELECT MAX(tp.transaction.transactionDate) FROM TransactionPayment tp WHERE tp.transaction.customer.id = :customerId")
    Optional<LocalDateTime> findLastPaymentDateByCustomerId(@Param("customerId") Long customerId);
}
