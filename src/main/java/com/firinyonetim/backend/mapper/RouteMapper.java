package com.firinyonetim.backend.mapper;
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
    RouteResponse toRouteResponse(Route route);

    @Mapping(target = "routeCode", ignore = true)
    @Mapping(target = "driver", ignore = true) // Serviste set edilecek
    void updateRouteFromDto(RouteUpdateRequest dto, @MappingTarget Route route);
}