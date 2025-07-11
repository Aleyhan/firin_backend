package com.firinyonetim.backend.dto.transaction.request;
import com.firinyonetim.backend.entity.ItemType;
import lombok.Data;
@Data public class TransactionItemRequest {
    private Long productId;
    private int quantity;
    private ItemType type; // "SATIS" veya "IADE"
}