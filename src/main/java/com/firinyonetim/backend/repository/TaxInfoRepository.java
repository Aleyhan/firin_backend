package com.firinyonetim.backend.repository;

import com.firinyonetim.backend.entity.TaxInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxInfoRepository extends JpaRepository<TaxInfo, Long> {
    // YENİ METOT: Belirtilen müşteri ID'si hariç, bu vergi numarasına sahip başka bir müşteri var mı?
    boolean existsByTaxNumberAndCustomerIdNot(String taxNumber, Long customerId);
}