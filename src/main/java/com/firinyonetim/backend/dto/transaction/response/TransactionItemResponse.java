package com.firinyonetim.backend.dto.transaction.response;

import com.firinyonetim.backend.entity.ItemType;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransactionItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private int quantity;
    private ItemType type;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
}