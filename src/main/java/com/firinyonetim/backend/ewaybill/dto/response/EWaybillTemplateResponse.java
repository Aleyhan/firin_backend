// src/main/java/com/firinyonetim/backend/ewaybill/dto/response/EWaybillTemplateResponse.java
package com.firinyonetim.backend.ewaybill.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;

@Data
public class EWaybillTemplateResponse {
    private Long customerId;
    private String notes;
    private String lastUpdatedByUsername;
    private String carrierName;
    private String carrierVknTckn;
    private String plateNumber;
    private Set<EWaybillTemplateItemResponse> items;
    private LocalDateTime updatedAt;
}