package com.firinyonetim.backend.repository;

import com.firinyonetim.backend.entity.ProductGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductGroupRepository extends JpaRepository<ProductGroup, Long> {
}