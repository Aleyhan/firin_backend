package com.firinyonetim.backend.dto.route;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteProductSummaryDto {
    private Long productId;
    private String productName;
    private int totalSold;
    private int totalReturned;
}