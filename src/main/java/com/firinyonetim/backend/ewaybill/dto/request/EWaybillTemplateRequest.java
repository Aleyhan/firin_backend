// src/main/java/com/firinyonetim/backend/ewaybill/dto/request/EWaybillTemplateRequest.java
package com.firinyonetim.backend.ewaybill.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

@Data
public class EWaybillTemplateRequest {
    private String notes;
    private String carrierName;
    private String carrierVknTckn;
    private String plateNumber;
    private Set<String> includedFields; // YENİ ALAN

    @NotEmpty
    @Valid
    private Set<EWaybillTemplateItemRequest> items;
}