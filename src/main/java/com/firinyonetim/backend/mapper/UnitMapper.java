package com.firinyonetim.backend.mapper;

import com.firinyonetim.backend.dto.product.UnitDto;
import com.firinyonetim.backend.entity.Unit;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UnitMapper {
    UnitDto toDto(Unit unit);
}