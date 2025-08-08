// src/main/java/com/firinyonetim/backend/ewaybill/dto/response/BulkSendResponseDto.java
package com.firinyonetim.backend.ewaybill.dto.response;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class BulkSendResponseDto {
    private int totalRequested;
    private int totalSuccess;
    private int totalFailed;
    private List<BulkSendResultDto> results = new ArrayList<>();
}