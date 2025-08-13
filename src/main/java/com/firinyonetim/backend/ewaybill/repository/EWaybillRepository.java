package com.firinyonetim.backend.ewaybill.repository;

import com.firinyonetim.backend.ewaybill.entity.EWaybill;
import com.firinyonetim.backend.ewaybill.entity.EWaybillStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EWaybillRepository extends JpaRepository<EWaybill, UUID>, JpaSpecificationExecutor<EWaybill> {

    @Query("SELECT e FROM EWaybill e WHERE e.status = 'SENDING' OR e.status = 'AWAITING_APPROVAL'")
    List<EWaybill> findEWaybillsToQueryStatus();

    // BU METODU TAMAMEN SİLİN
    /*
    @Query(value = "SELECT * FROM e_waybills e WHERE e.customer_id = :customerId AND e.status IN ('APPROVED', 'AWAITING_APPROVAL') " +
            "AND NOT EXISTS (" +
            "  SELECT 1 FROM invoices inv, jsonb_array_elements(inv.related_despatches_json) as elem " +
            "  WHERE (elem->>'id')::uuid = e.id" +
            ") ORDER BY e.ewaybill_number DESC", nativeQuery = true)
    List<EWaybill> findUninvoicedEWaybillsByCustomerId(@Param("customerId") Long customerId);
    */
}