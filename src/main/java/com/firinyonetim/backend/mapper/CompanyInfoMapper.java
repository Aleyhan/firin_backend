package com.firinyonetim.backend.mapper;

import com.firinyonetim.backend.dto.company_directory.CompanyInfoDto;
import com.firinyonetim.backend.entity.CompanyInfo;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CompanyInfoMapper {
    CompanyInfoDto toDto(CompanyInfo entity);
    void updateFromDto(CompanyInfoDto dto, @MappingTarget CompanyInfo entity);
}