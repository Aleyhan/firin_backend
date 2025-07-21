package com.firinyonetim.backend.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDto {
    private long customerCount;
    private long productCount;
    private long routeCount;
}