package com.firinyonetim.backend.invoice.mapper;

import com.firinyonetim.backend.invoice.dto.InvoiceSettingsDto;
import com.firinyonetim.backend.invoice.dto.InvoiceSettingsUpdateRequest;
import com.firinyonetim.backend.invoice.entity.InvoiceSettings;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface InvoiceSettingsMapper {

    InvoiceSettingsDto toDto(InvoiceSettings entity);

    void updateFromDto(InvoiceSettingsUpdateRequest dto, @MappingTarget InvoiceSettings entity);
}