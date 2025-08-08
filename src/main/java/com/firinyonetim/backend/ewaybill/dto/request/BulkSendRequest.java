// src/main/java/com/firinyonetim/backend/ewaybill/dto/request/BulkSendRequest.java
package com.firinyonetim.backend.ewaybill.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class BulkSendRequest {
    @NotEmpty
    private List<UUID> ewaybillIds;
}