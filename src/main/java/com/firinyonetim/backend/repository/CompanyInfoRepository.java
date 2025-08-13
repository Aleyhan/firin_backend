package com.firinyonetim.backend.repository;

import com.firinyonetim.backend.entity.CompanyInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyInfoRepository extends JpaRepository<CompanyInfo, Long> {}