package com.firinyonetim.backend.mapper;

import com.firinyonetim.backend.dto.transaction.response.TransactionResponse;
import com.firinyonetim.backend.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {TransactionItemMapper.class, TransactionPaymentMapper.class})
public interface TransactionMapper {

    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "customer.name", target = "customerName")
    @Mapping(source = "createdBy.id", target = "createdByUserId")
    @Mapping(source = "createdBy.username", target = "createdByUsername")
    TransactionResponse toTransactionResponse(Transaction transaction);
}