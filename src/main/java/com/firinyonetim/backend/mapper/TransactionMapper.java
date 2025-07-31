// src/main/java/com/firinyonetim/backend/mapper/TransactionMapper.java
package com.firinyonetim.backend.mapper;

import com.firinyonetim.backend.dto.transaction.response.TransactionResponse;
import com.firinyonetim.backend.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {TransactionItemMapper.class, TransactionPaymentMapper.class})
public interface TransactionMapper {

    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "customer.customerCode", target = "customerCode")
    @Mapping(source = "customer.name", target = "customerName")
    @Mapping(source = "createdBy.id", target = "createdByUserId")
    @Mapping(source = "createdBy.username", target = "createdByUsername")
    @Mapping(source = "route.id", target = "routeId")
    @Mapping(source = "route.name", target = "routeName")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "rejectionReason", target = "rejectionReason")
    @Mapping(target = "dailySequenceNumber", ignore = true)
    // YENİ MAPPING'LER
    @Mapping(source = "shipment.id", target = "shipmentId")
    @Mapping(source = "shipment.sequenceNumber", target = "shipmentSequenceNumber")
    @Mapping(source = "sequenceInShipment", target = "sequenceInShipment") // YENİ MAPPING
    TransactionResponse toTransactionResponse(Transaction transaction);
}