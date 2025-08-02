package com.firinyonetim.backend.repository.supplier;

import com.firinyonetim.backend.entity.supplier.InputProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InputProductRepository extends JpaRepository<InputProduct, Long> {
}
