package com.firinyonetim.backend.repository;

import com.firinyonetim.backend.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List; // YENİ IMPORT

public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {
    boolean existsByCustomerCode(String customerCode);
    boolean existsByCustomerCodeAndIdNot(String customerCode, Long id);

    // YENİ METOT
    List<Customer> findByTaxInfoTaxNumber(String taxNumber);
}