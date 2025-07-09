package com.firinyonetim.backend.repository;

import com.firinyonetim.backend.entity.SpecialProductPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SpecialProductPriceRepository extends JpaRepository<SpecialProductPrice, Long> {
    Optional<SpecialProductPrice> findByCustomerIdAndProductId(Long customerId, Long productId);
}