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
    Route toRoute(RouteCreateRequest request);
    RouteResponse toRouteResponse(Route route);

    // YENİ METOT: Var olan bir rotayı DTO'dan gelen verilerle günceller.
    // Rota kodunun güncellenmesini engellemek için ignore = true ekliyoruz.
    @Mapping(target = "routeCode", ignore = true)
    void updateRouteFromDto(RouteUpdateRequest dto, @MappingTarget Route route);


}