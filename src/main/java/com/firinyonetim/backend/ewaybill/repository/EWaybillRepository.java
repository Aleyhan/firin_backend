package com.firinyonetim.backend.ewaybill.repository;

import com.firinyonetim.backend.ewaybill.entity.EWaybill;
import com.firinyonetim.backend.ewaybill.entity.EWaybillStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EWaybillRepository extends JpaRepository<EWaybill, UUID>, JpaSpecificationExecutor<EWaybill> {

    // ESKİ METOT (Sadece SENDING durumunu buluyordu)
    // List<EWaybill> findByStatus(EWaybillStatus status);

    // YENİ METOT: Hem SENDING hem de AWAITING_APPROVAL durumundakileri bulur.
    @Query("SELECT e FROM EWaybill e WHERE e.status = 'SENDING' OR e.status = 'AWAITING_APPROVAL'")
    List<EWaybill> findEWaybillsToQueryStatus();

}