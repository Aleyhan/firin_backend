package com.firinyonetim.backend.repository;

import com.firinyonetim.backend.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {
    // Müşteri kodunun benzersizliğini kontrol etmek için
    boolean existsByCustomerCode(String customerCode);

    // YENİ METOT: Belirtilen ID hariç, bu müşteri koduna sahip başka bir müşteri var mı?
    boolean existsByCustomerCodeAndIdNot(String customerCode, Long id);
}