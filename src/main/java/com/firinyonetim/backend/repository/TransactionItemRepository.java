package com.firinyonetim.backend.repository;
import com.firinyonetim.backend.entity.TransactionItem;
import org.springframework.data.jpa.repository.JpaRepository;


public interface TransactionItemRepository extends JpaRepository<TransactionItem, Long> {
}
