// src/main/java/com/firinyonetim/backend/dto/driver/response/DriverTodaysTransactionDto.java
package com.firinyonetim.backend.dto.driver.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class DriverTodaysTransactionDto {
    private Long transactionId;
    private LocalDateTime transactionTime;
    private List<DriverTodaysTransactionItemDto> items;
}