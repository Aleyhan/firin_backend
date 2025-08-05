package com.firinyonetim.backend.ewaybill.mapper;

import com.firinyonetim.backend.ewaybill.dto.request.EWaybillCreateRequest;
import com.firinyonetim.backend.ewaybill.dto.response.EWaybillItemResponse;
import com.firinyonetim.backend.ewaybill.dto.response.EWaybillResponse;
import com.firinyonetim.backend.ewaybill.dto.turkcell.TurkcellApiRequest;
import com.firinyonetim.backend.ewaybill.entity.EWaybill;
import com.firinyonetim.backend.ewaybill.entity.EWaybillItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface EWaybillMapper {

    // --- Turkcell API DTO'suna Dönüşüm (Basitleştirilmiş) ---
    @Mapping(target = "status", constant = "20")
    @Mapping(target = "isNew", constant = "true")
    @Mapping(target = "useManualDespatchAdviceId", constant = "false")
    @Mapping(source = "issueDate", target = "generalInfo.issueDate")
    @Mapping(source = "issueTime", target = "generalInfo.issueTime")
    @Mapping(source = "shipmentDate", target = "orderInfo.shipmentDate", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Mapping(source = "items", target = "despatchLines")
    // Adres ve Müşteri bilgileri gibi karmaşık alanlar serviste manuel olarak set edilecek
    @Mapping(target = "addressBook", ignore = true)
    @Mapping(target = "deliveryAddressInfo", ignore = true)
    @Mapping(target = "despatchShipmentInfo", ignore = true)
    @Mapping(target = "despatchBuyerCustomerInfo", ignore = true)
    @Mapping(target = "sellerSupplierInfo", ignore = true)
    TurkcellApiRequest toTurkcellApiRequest(EWaybill eWaybill);

    @Mapping(source = "productNameSnapshot", target = "productName")
    @Mapping(source = "quantity", target = "amount")
    TurkcellApiRequest.DespatchLine eWaybillItemToDespatchLine(EWaybillItem item);

    // --- Bizim API DTO'larımıza Dönüşüm ---
    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "customer.name", target = "customerName")
    @Mapping(source = "createdBy.username", target = "createdByUsername")
    EWaybillResponse toResponseDto(EWaybill eWaybill);

    @Mapping(source = "product.id", target = "productId")
    EWaybillItemResponse itemToItemResponseDto(EWaybillItem item);

    // --- Request DTO'dan Entity'ye Dönüşüm ---
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "turkcellApiId", ignore = true)
    @Mapping(target = "ewaybillNumber", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "turkcellStatus", ignore = true)
    @Mapping(target = "statusMessage", ignore = true)
    @Mapping(target = "deliveryAddressJson", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    EWaybill fromCreateRequest(EWaybillCreateRequest dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "turkcellApiId", ignore = true)
    @Mapping(target = "ewaybillNumber", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "turkcellStatus", ignore = true)
    @Mapping(target = "statusMessage", ignore = true)
    @Mapping(target = "deliveryAddressJson", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateFromRequest(EWaybillCreateRequest dto, @MappingTarget EWaybill eWaybill);
}