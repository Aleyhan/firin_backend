// src/main/java/com/firinyonetim/backend/mapper/RouteMapper.java
package com.firinyonetim.backend.mapper;
import com.firinyonetim.backend.dto.route.RouteSummaryDto;
import com.firinyonetim.backend.dto.route.request.RouteCreateRequest;
import com.firinyonetim.backend.dto.route.request.RouteUpdateRequest;
import com.firinyonetim.backend.dto.route.response.RouteResponse;
import com.firinyonetim.backend.entity.Route;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface RouteMapper {
    @Mapping(target = "driver", ignore = true) // Serviste set edilecek
    Route toRoute(RouteCreateRequest request);

    @Mapping(source = "driver.id", target = "driverId")
    @Mapping(source = "driver.name", target = "driverName")
    @Mapping(target = "customerCount", ignore = true) // Bu alanlar summary'den gelecek
    @Mapping(target = "totalDebt", ignore = true)     // Bu alanlar summary'den gelecek
    RouteResponse toRouteResponse(Route route);

    @Mapping(target = "routeCode", ignore = true)
    @Mapping(target = "driver", ignore = true) // Serviste set edilecek
    void updateRouteFromDto(RouteUpdateRequest dto, @MappingTarget Route route);

    // YENİ METOT: RouteSummaryDto'dan RouteResponse'a dönüşüm
    @Mapping(source = "route.id", target = "id")
    @Mapping(source = "route.routeCode", target = "routeCode")
    @Mapping(source = "route.name", target = "name")
    @Mapping(source = "route.description", target = "description")
    @Mapping(source = "route.active", target = "active")
    @Mapping(source = "route.plaka", target = "plaka")
    @Mapping(source = "route.driver.id", target = "driverId")
    @Mapping(source = "route.driver.name", target = "driverName")
    @Mapping(source = "customerCount", target = "customerCount")
    @Mapping(source = "totalDebt", target = "totalDebt")
    RouteResponse toRouteResponseFromSummary(RouteSummaryDto summaryDto);
}