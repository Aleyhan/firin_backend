package com.firinyonetim.backend.mapper;
import com.firinyonetim.backend.dto.route.request.RouteCreateRequest;
import com.firinyonetim.backend.dto.route.response.RouteResponse;
import com.firinyonetim.backend.entity.Route;
import org.mapstruct.Mapper;
@Mapper(componentModel = "spring")
public interface RouteMapper {
    Route toRoute(RouteCreateRequest request);
    RouteResponse toRouteResponse(Route route);
}