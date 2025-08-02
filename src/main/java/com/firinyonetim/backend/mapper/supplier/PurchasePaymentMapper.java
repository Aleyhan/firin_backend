package com.firinyonetim.backend.mapper.supplier;

import com.firinyonetim.backend.dto.supplier.response.PurchasePaymentResponse;
import com.firinyonetim.backend.entity.supplier.PurchasePayment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PurchasePaymentMapper {

    @Mapping(source = "supplier.id", target = "supplierId")
    @Mapping(source = "supplier.name", target = "supplierName")
    @Mapping(source = "createdBy.username", target = "createdByUsername")
    PurchasePaymentResponse toPurchasePaymentResponse(PurchasePayment payment);
}
