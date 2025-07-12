package com.firinyonetim.backend.repository;

import com.firinyonetim.backend.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    // Müşteri kodunun benzersizliğini kontrol etmek için
    boolean existsByCustomerCode(String customerCode);
}