// src/main/java/com/firinyonetim/backend/repository/TransactionRepository.java
package com.firinyonetim.backend.repository;

import com.firinyonetim.backend.entity.Transaction;
import com.firinyonetim.backend.entity.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    // ... (diğer metotlar aynı)
    @Query("SELECT DISTINCT t FROM Transaction t " +
            "LEFT JOIN FETCH t.items " +
            "LEFT JOIN FETCH t.payments " +
            "WHERE t.customer.id = :customerId " +
            "ORDER BY t.transactionDate DESC")
    List<Transaction> findByCustomerIdOrderByTransactionDateDesc(Long customerId);

    @Query("SELECT DISTINCT t FROM Transaction t " +
            "LEFT JOIN FETCH t.items item " +
            "LEFT JOIN FETCH item.product " +
            "LEFT JOIN FETCH t.payments " +
            "LEFT JOIN FETCH t.customer " +
            "LEFT JOIN FETCH t.createdBy " +
            "LEFT JOIN FETCH t.route " +
            "LEFT JOIN FETCH t.shipment " + // shipment'ı da fetch et
            "WHERE t.customer.id = :customerId " +
            "ORDER BY t.transactionDate ASC")
    List<Transaction> findByCustomerIdOrderByTransactionDateAsc(@Param("customerId") Long customerId);


    @Query("SELECT t FROM Transaction t " +
            "LEFT JOIN FETCH t.items " +
            "LEFT JOIN FETCH t.payments " +
            "LEFT JOIN FETCH t.shipment " + // shipment'ı da fetch et
            "WHERE t.id = :transactionId")
    Optional<Transaction> findByIdWithDetails(Long transactionId);

    @Query("SELECT t FROM Transaction t " +
            "JOIN FETCH t.customer " +
            "JOIN FETCH t.createdBy " +
            "LEFT JOIN FETCH t.route " +
            "LEFT JOIN FETCH t.shipment " + // shipment'ı da fetch et
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
            "LEFT JOIN FETCH t.shipment " + // shipment'ı da fetch et
            "LEFT JOIN FETCH t.items ti " +
            "LEFT JOIN FETCH ti.product " +
            "LEFT JOIN FETCH t.payments " +
            "ORDER BY t.transactionDate DESC LIMIT 10")
    List<Transaction> findTop10ByOrderByTransactionDateDesc();

    List<Transaction> findByStatusOrderByTransactionDateAsc(TransactionStatus status);

    @Query("SELECT t FROM Transaction t " +
            "JOIN FETCH t.items i " +
            "JOIN FETCH i.product " +
            "WHERE t.customer.id = :customerId " +
            "AND t.createdBy.id = :driverId " +
            "AND t.transactionDate >= :startOfDay AND t.transactionDate < :endOfDay")
    List<Transaction> findTodaysTransactionsByCustomerAndDriver(
            @Param("customerId") Long customerId,
            @Param("driverId") Long driverId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );

    @Query("SELECT t FROM Transaction t JOIN FETCH t.items i JOIN FETCH i.product WHERE t.shipment.id = :shipmentId")
    List<Transaction> findByShipmentId(Long shipmentId);

    // YENİ METOT
    long countByShipmentId(Long shipmentId);

    // YENİ METOT: Belirli bir tarihten önceki tüm onaylanmış işlemleri getirir.
    @Query("SELECT t FROM Transaction t " +
            "LEFT JOIN FETCH t.items " +
            "LEFT JOIN FETCH t.payments " +
            "WHERE t.customer.id IN :customerIds AND t.status = 'APPROVED' AND t.transactionDate < :date")
    List<Transaction> findApprovedTransactionsForCustomersBeforeDate(
            @Param("customerIds") List<Long> customerIds,
            @Param("date") LocalDateTime date
    );

}