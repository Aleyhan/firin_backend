package com.firinyonetim.backend.mapper;

import com.firinyonetim.backend.dto.transaction.response.TransactionResponse;
import com.firinyonetim.backend.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {TransactionItemMapper.class, TransactionPaymentMapper.class})
public interface TransactionMapper {

    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "customer.customerCode", target = "customerCode") // YENİ MAPPING
    @Mapping(source = "customer.name", target = "customerName")
    @Mapping(source = "createdBy.id", target = "createdByUserId")
    @Mapping(source = "createdBy.username", target = "createdByUsername")
    @Mapping(source = "route.id", target = "routeId")
    @Mapping(source = "route.name", target = "routeName")
    TransactionResponse toTransactionResponse(Transaction transaction);
}