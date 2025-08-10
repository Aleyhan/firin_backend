package com.firinyonetim.backend.invoice.mapper;

import com.firinyonetim.backend.invoice.dto.InvoiceResponse;
import com.firinyonetim.backend.invoice.entity.Invoice;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {InvoiceItemMapper.class})
public interface InvoiceMapper {

    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "customer.name", target = "customerName")
    @Mapping(source = "createdBy.username", target = "createdByUsername")
    InvoiceResponse toResponse(Invoice entity);
}