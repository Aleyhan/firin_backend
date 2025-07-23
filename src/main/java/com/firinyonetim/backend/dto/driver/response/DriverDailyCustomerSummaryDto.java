// src/main/java/com/firinyonetim/backend/dto/driver/response/DriverDailyCustomerSummaryDto.java
package com.firinyonetim.backend.dto.driver.response;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class DriverDailyCustomerSummaryDto {
    // Satışları işlem bazında tutacak liste
    private List<DriverTodaysTransactionDto> salesTransactions;
    // İadeleri ürün bazında toplayacak map
    private Map<String, Integer> totalReturnsByProduct;
}