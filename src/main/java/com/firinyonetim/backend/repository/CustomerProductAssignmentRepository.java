package com.firinyonetim.backend.repository;

import com.firinyonetim.backend.entity.CustomerProductAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CustomerProductAssignmentRepository extends JpaRepository<CustomerProductAssignment, Long> {

    Optional<CustomerProductAssignment> findByCustomerIdAndProductId(Long customerId, Long productId);
    void deleteByCustomerIdAndProductId(Long customerId, Long productId);
    List<CustomerProductAssignment> findByCustomerId(Long customerId);
    List<CustomerProductAssignment> findByProductIdAndSpecialPriceIsNotNull(Long productId);

    // YENİ METOT
    List<CustomerProductAssignment> findByCustomerIdIn(List<Long> customerIds);
}