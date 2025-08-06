package com.firinyonetim.backend.mapper;

import com.firinyonetim.backend.dto.product.UnitDto;
import com.firinyonetim.backend.entity.Unit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UnitMapper {
    @Mapping(source = "code", target = "code") // YENİ MAPPING
    UnitDto toDto(Unit unit);
}