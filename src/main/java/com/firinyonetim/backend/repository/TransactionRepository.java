package com.firinyonetim.backend.repository;

import com.firinyonetim.backend.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    @Query("SELECT DISTINCT t FROM Transaction t " +
            "LEFT JOIN FETCH t.items " +
            "LEFT JOIN FETCH t.payments " +
            "WHERE t.customer.id = :customerId " +
            "ORDER BY t.transactionDate DESC")
    List<Transaction> findByCustomerIdOrderByTransactionDateDesc(Long customerId);

    @Query("SELECT t FROM Transaction t " +
            "LEFT JOIN FETCH t.items " +
            "LEFT JOIN FETCH t.payments " +
            "WHERE t.id = :transactionId")
    Optional<Transaction> findByIdWithDetails(Long transactionId);

    // DEĞİŞİKLİK: Sorgu güncellendi.
    @Query("SELECT t FROM Transaction t " +
            "JOIN FETCH t.customer " +
            "JOIN FETCH t.createdBy " +
            "LEFT JOIN FETCH t.route " +
            "LEFT JOIN FETCH t.items ti " +
            "LEFT JOIN FETCH ti.product " +
            "LEFT JOIN FETCH t.payments " +
            "WHERE t.transactionDate >= :startOfDay AND t.transactionDate < :startOfNextDay")
    List<Transaction> findTransactionsBetween(@Param("startOfDay") LocalDateTime startOfDay, @Param("startOfNextDay") LocalDateTime startOfNextDay);

    @Query(value = "SELECT * FROM transactions t WHERE CAST(t.transaction_date AS date) = :date", nativeQuery = true)
    List<Transaction> findTransactionsByDate(@Param("date") LocalDate date);

    @Query("SELECT t FROM Transaction t " +
            "JOIN FETCH t.customer " +
            "JOIN FETCH t.createdBy " +
            "LEFT JOIN FETCH t.route " +
            "LEFT JOIN FETCH t.items ti " +
            "LEFT JOIN FETCH ti.product " +
            "LEFT JOIN FETCH t.payments " +
            "ORDER BY t.transactionDate DESC LIMIT 10")
    List<Transaction> findTop10ByOrderByTransactionDateDesc();
}