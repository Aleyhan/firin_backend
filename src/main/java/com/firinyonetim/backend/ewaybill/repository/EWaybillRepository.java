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

    @Query("SELECT e FROM EWaybill e WHERE e.status = 'SENDING' OR e.status = 'AWAITING_APPROVAL' OR e.status = 'APPROVING' OR e.status = 'REJECTING'")
    List<EWaybill> findEWaybillsToQueryStatus();

}