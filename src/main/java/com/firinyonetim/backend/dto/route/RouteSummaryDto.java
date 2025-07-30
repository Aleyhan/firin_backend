// src/main/java/com/firinyonetim/backend/dto/route/RouteSummaryDto.java
package com.firinyonetim.backend.dto.route;

import com.firinyonetim.backend.entity.Route;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteSummaryDto {
    private Route route;
    private Long customerCount;
    private BigDecimal totalDebt;
}