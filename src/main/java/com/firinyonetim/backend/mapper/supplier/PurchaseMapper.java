package com.firinyonetim.backend.mapper.supplier;
import com.firinyonetim.backend.dto.supplier.response.PurchaseItemResponse;
import com.firinyonetim.backend.dto.supplier.response.PurchaseResponse;
import com.firinyonetim.backend.entity.supplier.Purchase;
import com.firinyonetim.backend.entity.supplier.PurchaseItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
@Mapper(componentModel = "spring")
public interface PurchaseMapper {
    @Mapping(source = "supplier.id", target = "supplierId")
    @Mapping(source = "supplier.name", target = "supplierName")
    @Mapping(source = "createdBy.username", target = "createdByUsername")
    PurchaseResponse toPurchaseResponse(Purchase purchase);

    @Mapping(source = "inputProduct.id", target = "inputProductId")
    @Mapping(source = "inputProduct.name", target = "inputProductName")
    @Mapping(source = "inputProduct.unit", target = "inputProductUnit")
    PurchaseItemResponse toPurchaseItemResponse(PurchaseItem item);
}