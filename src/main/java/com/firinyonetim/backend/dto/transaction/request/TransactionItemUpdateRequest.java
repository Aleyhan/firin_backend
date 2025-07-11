package com.firinyonetim.backend.dto.transaction.request;
import com.firinyonetim.backend.entity.ItemType;
import lombok.Data;
@Data public class TransactionItemUpdateRequest {
    private Long id; // Güncellenecek kalemin ID'si (yeni ise null)
    private Long productId;
    private int quantity;
    private ItemType type;
}