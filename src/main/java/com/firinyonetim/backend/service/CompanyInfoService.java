package com.firinyonetim.backend.service;

import com.firinyonetim.backend.dto.company_directory.CompanyInfoDto;
import com.firinyonetim.backend.entity.CompanyInfo;
import com.firinyonetim.backend.mapper.CompanyInfoMapper;
import com.firinyonetim.backend.repository.CompanyInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompanyInfoService {

    private final CompanyInfoRepository companyInfoRepository;
    private final CompanyInfoMapper companyInfoMapper;
    private static final Long COMPANY_INFO_ID = 1L;

    @Transactional
    public CompanyInfo getCompanyInfoEntity() {
        return companyInfoRepository.findById(COMPANY_INFO_ID)
                .orElseGet(() -> {
                    CompanyInfo newInfo = new CompanyInfo();
                    newInfo.setId(COMPANY_INFO_ID);
                    // Varsayılan değerler atayabiliriz
                    newInfo.setIdentificationNumber("");
                    newInfo.setName("");
                    return companyInfoRepository.save(newInfo);
                });
    }

    @Transactional(readOnly = true)
    public CompanyInfoDto getCompanyInfo() {
        return companyInfoMapper.toDto(getCompanyInfoEntity());
    }

    @Transactional
    public CompanyInfoDto updateCompanyInfo(CompanyInfoDto dto) {
        CompanyInfo companyInfo = getCompanyInfoEntity();
        companyInfoMapper.updateFromDto(dto, companyInfo);
        CompanyInfo updatedInfo = companyInfoRepository.save(companyInfo);
        return companyInfoMapper.toDto(updatedInfo);
    }
}