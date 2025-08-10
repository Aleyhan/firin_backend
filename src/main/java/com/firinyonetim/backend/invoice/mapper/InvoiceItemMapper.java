package com.firinyonetim.backend.invoice.mapper;

import com.firinyonetim.backend.invoice.dto.InvoiceItemResponse;
import com.firinyonetim.backend.invoice.entity.InvoiceItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InvoiceItemMapper {

    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productNameSnapshot")
    InvoiceItemResponse toResponse(InvoiceItem entity);
}