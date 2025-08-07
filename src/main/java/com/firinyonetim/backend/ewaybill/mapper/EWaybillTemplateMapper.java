// src/main/java/com/firinyonetim/backend/ewaybill/mapper/EWaybillTemplateMapper.java
package com.firinyonetim.backend.ewaybill.mapper;

import com.firinyonetim.backend.ewaybill.dto.request.EWaybillTemplateRequest;
import com.firinyonetim.backend.ewaybill.dto.response.EWaybillTemplateItemResponse;
import com.firinyonetim.backend.ewaybill.dto.response.EWaybillTemplateResponse;
import com.firinyonetim.backend.ewaybill.entity.EWaybillTemplate;
import com.firinyonetim.backend.ewaybill.entity.EWaybillTemplateItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface EWaybillTemplateMapper {

    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "lastUpdatedBy.username", target = "lastUpdatedByUsername")
    EWaybillTemplateResponse toResponse(EWaybillTemplate template);

    @Mapping(source = "product.id", target = "productId")
    EWaybillTemplateItemResponse itemToItemResponse(EWaybillTemplateItem item);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "lastUpdatedBy", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateFromRequest(EWaybillTemplateRequest dto, @MappingTarget EWaybillTemplate template);
}