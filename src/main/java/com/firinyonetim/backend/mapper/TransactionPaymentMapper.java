package com.firinyonetim.backend.mapper;

import com.firinyonetim.backend.dto.transaction.response.TransactionPaymentResponse;
import com.firinyonetim.backend.entity.TransactionPayment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TransactionPaymentMapper {
    TransactionPaymentResponse toTransactionPaymentResponse(TransactionPayment transactionPayment);
}