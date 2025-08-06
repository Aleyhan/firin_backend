package com.firinyonetim.backend.ewaybill.mapper;

import com.firinyonetim.backend.ewaybill.dto.response.EWaybillCustomerInfoResponse;
import com.firinyonetim.backend.ewaybill.entity.EWaybillCustomerInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EWaybillCustomerInfoMapper {
    @Mapping(source = "customer.id", target = "customerId")
    EWaybillCustomerInfoResponse toResponse(EWaybillCustomerInfo info);
}