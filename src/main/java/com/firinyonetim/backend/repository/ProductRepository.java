package com.firinyonetim.backend.repository;

import com.firinyonetim.backend.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    // YENİ METOTLAR
    boolean existsByProductGroupId(Long productGroupId);
    boolean existsByUnitId(Long unitId);
}