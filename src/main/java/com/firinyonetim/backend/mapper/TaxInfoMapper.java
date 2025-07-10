package com.firinyonetim.backend.mapper;

import com.firinyonetim.backend.dto.tax_info.request.TaxInfoRequest;
import com.firinyonetim.backend.dto.tax_info.response.TaxInfoResponse;
import com.firinyonetim.backend.entity.TaxInfo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TaxInfoMapper {

    TaxInfo toTaxInfo(TaxInfoRequest request);

    TaxInfoResponse toTaxInfoResponse(TaxInfo taxInfo);
}