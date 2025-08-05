package com.firinyonetim.backend.ewaybill.repository;

import com.firinyonetim.backend.ewaybill.entity.EWaybillItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EWaybillItemRepository extends JpaRepository<EWaybillItem, Long> {
}