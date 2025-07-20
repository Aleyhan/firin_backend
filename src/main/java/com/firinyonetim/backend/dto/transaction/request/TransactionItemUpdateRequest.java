package com.firinyonetim.backend.dto.transaction.request;
import com.firinyonetim.backend.entity.ItemType;
import lombok.Data;
import java.math.BigDecimal; // BigDecimal import'u eklendi

@Data
public class TransactionItemUpdateRequest {
    private Long id; // Güncellenecek kalemin ID'si
    private Long productId;
    private int quantity;
    private ItemType type;
    private BigDecimal unitPrice; // Birim fiyatı da güncelleyebilmek için
}