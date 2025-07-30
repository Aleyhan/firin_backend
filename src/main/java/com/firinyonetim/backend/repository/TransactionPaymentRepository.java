// src/main/java/com/firinyonetim/backend/repository/TransactionPaymentRepository.java
package com.firinyonetim.backend.repository;

import com.firinyonetim.backend.entity.TransactionPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TransactionPaymentRepository extends JpaRepository<TransactionPayment, Long> {
    @Query("SELECT MAX(tp.transaction.transactionDate) FROM TransactionPayment tp WHERE tp.transaction.customer.id = :customerId")
    Optional<LocalDateTime> findLastPaymentDateByCustomerId(@Param("customerId") Long customerId);

    // YENİ METOT
    @Query("SELECT tp.transaction.customer.id as customerId, MAX(tp.transaction.transactionDate) as lastPaymentDate " +
            "FROM TransactionPayment tp " +
            "WHERE tp.transaction.customer.id IN :customerIds " +
            "GROUP BY tp.transaction.customer.id")
    List<Map<String, Object>> findLastPaymentDatesForCustomerIds(@Param("customerIds") List<Long> customerIds);
}