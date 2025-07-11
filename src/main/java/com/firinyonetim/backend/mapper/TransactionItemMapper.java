package com.firinyonetim.backend.mapper;

import com.firinyonetim.backend.dto.transaction.response.TransactionItemResponse;
import com.firinyonetim.backend.entity.TransactionItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionItemMapper {
    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    TransactionItemResponse toTransactionItemResponse(TransactionItem transactionItem);
}