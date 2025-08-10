package com.firinyonetim.backend.invoice.repository;

import com.firinyonetim.backend.invoice.entity.InvoiceSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoiceSettingsRepository extends JpaRepository<InvoiceSettings, Long> {
    Optional<InvoiceSettings> findFirstById(Long id);
}
