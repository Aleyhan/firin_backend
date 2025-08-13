package com.firinyonetim.backend.controller;

import com.firinyonetim.backend.dto.company_directory.CompanyInfoDto;
import com.firinyonetim.backend.service.CompanyInfoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/company-info")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('YONETICI', 'DEVELOPER', 'MUHASEBE')")
public class CompanyInfoController {

    private final CompanyInfoService companyInfoService;

    @GetMapping
    public ResponseEntity<CompanyInfoDto> getCompanyInfo() {
        return ResponseEntity.ok(companyInfoService.getCompanyInfo());
    }

    @PutMapping
    public ResponseEntity<CompanyInfoDto> updateCompanyInfo(@Valid @RequestBody CompanyInfoDto dto) {
        return ResponseEntity.ok(companyInfoService.updateCompanyInfo(dto));
    }
}