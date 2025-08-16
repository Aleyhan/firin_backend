// src/main/java/com/firinyonetim/backend/invoice/mapper/InvoiceMapper.java
package com.firinyonetim.backend.invoice.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.firinyonetim.backend.ewaybill.entity.EWaybill;
import com.firinyonetim.backend.ewaybill.mapper.EWaybillMapper;
import com.firinyonetim.backend.ewaybill.repository.EWaybillRepository;
import com.firinyonetim.backend.invoice.dto.EWaybillForInvoiceDto;
import com.firinyonetim.backend.invoice.dto.InvoiceResponse;
import com.firinyonetim.backend.invoice.entity.Invoice;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Mapper(componentModel = "spring", uses = {InvoiceItemMapper.class, EWaybillMapper.class})
public abstract class InvoiceMapper {

    @Autowired
    private EWaybillRepository eWaybillRepository;
    @Autowired
    private EWaybillMapper eWaybillMapper;
    @Autowired
    private ObjectMapper objectMapper;

    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "customer.name", target = "customerName")
    @Mapping(source = "customer.customerCode", target = "customerCode")
    @Mapping(source = "createdBy.username", target = "createdByUsername")
    @Mapping(target = "relatedDespatchesJson", ignore = true) // Doğrudan map'lemeyi iptal et
    public abstract InvoiceResponse toResponse(Invoice entity);

    @AfterMapping
    protected void afterToResponse(Invoice entity, @MappingTarget InvoiceResponse response) {
        if (!StringUtils.hasText(entity.getRelatedDespatchesJson())) {
            response.setRelatedDespatchesJson(null);
            return;
        }

        try {
            // 1. JSON'dan sadece irsaliye ID'lerini al
            // DEĞİŞİKLİK BURADA: Map<String, String> -> Map<String, Object>
            List<Map<String, Object>> despatchesInfo = objectMapper.readValue(entity.getRelatedDespatchesJson(), new TypeReference<>() {});
            List<UUID> ewaybillIds = despatchesInfo.stream()
                    .map(d -> UUID.fromString((String) d.get("id")))
                    .collect(Collectors.toList());

            if (ewaybillIds.isEmpty()) {
                response.setRelatedDespatchesJson("[]");
                return;
            }

            // 2. Veritabanından bu ID'lere ait irsaliyeleri tüm detaylarıyla (items dahil) çek
            List<EWaybill> ewaybills = eWaybillRepository.findAllById(ewaybillIds);

            // 3. Çekilen güncel verilerle DTO listesini oluştur
            List<EWaybillForInvoiceDto> relatedDespatchesDtos = ewaybills.stream()
                    .map(ew -> {
                        EWaybillForInvoiceDto dto = new EWaybillForInvoiceDto();
                        dto.setId(ew.getId());
                        dto.setDespatchNumber(ew.getEwaybillNumber());
                        dto.setIssueDate(ew.getIssueDate().atTime(ew.getIssueTime()));
                        dto.setCustomerName(ew.getCustomer().getName());
                        dto.setItems(ew.getItems().stream()
                                .map(eWaybillMapper::itemToItemResponseDto)
                                .collect(Collectors.toList()));
                        return dto;
                    })
                    .collect(Collectors.toList());

            // 4. Oluşturulan güncel DTO listesini JSON'a çevirip response'a ekle
            response.setRelatedDespatchesJson(objectMapper.writeValueAsString(relatedDespatchesDtos));

        } catch (JsonProcessingException e) {
            log.error("Could not process relatedDespatchesJson for invoice {}", entity.getId(), e);
            response.setRelatedDespatchesJson(null);
        }
    }
}