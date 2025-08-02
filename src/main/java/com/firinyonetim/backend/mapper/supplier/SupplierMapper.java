package com.firinyonetim.backend.mapper.supplier;

import com.firinyonetim.backend.dto.supplier.request.SupplierRequest;
import com.firinyonetim.backend.dto.supplier.response.SupplierResponse;
import com.firinyonetim.backend.entity.supplier.Supplier;
import com.firinyonetim.backend.mapper.AddressMapper;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

// SupplierTaxInfoMapper eklendi
@Mapper(componentModel = "spring", uses = {AddressMapper.class, SupplierTaxInfoMapper.class})
public interface SupplierMapper {

    SupplierResponse toSupplierResponse(Supplier supplier);

    Supplier toSupplier(SupplierRequest request);

    void updateSupplierFromDto(SupplierRequest request, @MappingTarget Supplier supplier);
}