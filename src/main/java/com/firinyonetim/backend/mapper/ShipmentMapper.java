// src/main/java/com/firinyonetim/backend/mapper/ShipmentMapper.java
package com.firinyonetim.backend.mapper;

import com.firinyonetim.backend.dto.shipment.response.ShipmentResponse;
import com.firinyonetim.backend.entity.Shipment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ShipmentMapper {

    @Mapping(source = "route.id", target = "routeId")
    @Mapping(source = "driver.id", target = "driverId")
    @Mapping(source = "status", target = "status") // YENİ MAPPING
    ShipmentResponse toResponse(Shipment shipment);
}