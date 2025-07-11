package com.firinyonetim.backend.repository;

import com.firinyonetim.backend.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // Query import'u eklendi

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("SELECT DISTINCT t FROM Transaction t " + // DISTINCT eklendi
            "LEFT JOIN FETCH t.items " +
            "LEFT JOIN FETCH t.payments " +
            "WHERE t.customer.id = :customerId " +
            "ORDER BY t.transactionDate DESC")
    List<Transaction> findByCustomerIdOrderByTransactionDateDesc(Long customerId);

    // ... TransactionRepository arayüzünün içinde ...

    @Query("SELECT t FROM Transaction t " +
            "LEFT JOIN FETCH t.items " +
            "LEFT JOIN FETCH t.payments " +
            "WHERE t.id = :transactionId")
    Optional<Transaction> findByIdWithDetails(Long transactionId);
}