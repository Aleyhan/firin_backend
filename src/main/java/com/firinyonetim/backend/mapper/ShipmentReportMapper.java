// src/main/java/com/firinyonetim/backend/mapper/ShipmentReportMapper.java
package com.firinyonetim.backend.mapper;

import com.firinyonetim.backend.dto.shipment.response.ShipmentReportResponse;
import com.firinyonetim.backend.entity.Shipment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {ShipmentItemReportMapper.class})
public interface ShipmentReportMapper {
    @Mapping(source = "route.name", target = "routeName")
    @Mapping(source = "driver.name", target = "driverName")
    ShipmentReportResponse toResponse(Shipment shipment);
}