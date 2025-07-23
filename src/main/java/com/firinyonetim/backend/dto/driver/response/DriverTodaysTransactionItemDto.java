// src/main/java/com/firinyonetim/backend/dto/driver/response/DriverTodaysTransactionItemDto.java
package com.firinyonetim.backend.dto.driver.response;

import lombok.Data;

@Data
public class DriverTodaysTransactionItemDto {
    private String productName;
    private int totalSold = 0;
    private int totalReturned = 0;

    public DriverTodaysTransactionItemDto(String productName) {
        this.productName = productName;
    }
}