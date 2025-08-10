package com.firinyonetim.backend.invoice.repository;

import com.firinyonetim.backend.invoice.entity.Invoice;
import com.firinyonetim.backend.invoice.entity.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID>, JpaSpecificationExecutor<Invoice> {
    List<Invoice> findByStatus(InvoiceStatus status);
}