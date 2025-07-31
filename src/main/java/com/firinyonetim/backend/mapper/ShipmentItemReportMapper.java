// src/main/java/com/firinyonetim/backend/mapper/ShipmentItemReportMapper.java
package com.firinyonetim.backend.mapper;

import com.firinyonetim.backend.dto.shipment.response.ShipmentItemReportDto;
import com.firinyonetim.backend.entity.ShipmentItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ShipmentItemReportMapper {
    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
        // Diğer alanlar aynı isimde olduğu için otomatik map'lenecek
    ShipmentItemReportDto toDto(ShipmentItem shipmentItem);
}