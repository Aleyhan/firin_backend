// src/main/java/com/firinyonetim/backend/dto/driver/response/DriverDailySummaryItemDto.java
package com.firinyonetim.backend.dto.driver.response;

import lombok.Data;

@Data
public class DriverDailySummaryItemDto {
    private String productName;
    private int totalSold = 0;
    private int totalReturned = 0;

    public DriverDailySummaryItemDto(String productName) {
        this.productName = productName;
    }
}