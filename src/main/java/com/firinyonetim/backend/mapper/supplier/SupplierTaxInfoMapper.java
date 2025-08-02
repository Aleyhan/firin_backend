package com.firinyonetim.backend.mapper.supplier;

import com.firinyonetim.backend.dto.supplier.request.SupplierTaxInfoRequest;
import com.firinyonetim.backend.dto.supplier.response.SupplierTaxInfoResponse;
import com.firinyonetim.backend.entity.supplier.SupplierTaxInfo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SupplierTaxInfoMapper {
    SupplierTaxInfo toSupplierTaxInfo(SupplierTaxInfoRequest request);
    SupplierTaxInfoResponse toSupplierTaxInfoResponse(SupplierTaxInfo taxInfo); // YENİ METOT
}