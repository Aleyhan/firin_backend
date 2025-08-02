package com.firinyonetim.backend.repository.supplier;

import com.firinyonetim.backend.entity.supplier.SupplierTaxInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupplierTaxInfoRepository extends JpaRepository<SupplierTaxInfo, Long> {
    boolean existsByTaxNumber(String taxNumber);
    boolean existsByTaxNumberAndIdNot(String taxNumber, Long id);
}