package com.firinyonetim.backend.ewaybill.dto.response;

import com.firinyonetim.backend.ewaybill.entity.EWaybillStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

@Data
public class EWaybillResponse {
    private UUID id;
    private String turkcellApiId;
    private String ewaybillNumber;
    private EWaybillStatus status;
    private Integer turkcellStatus;
    private String statusMessage;
    private LocalDate issueDate;
    private LocalTime issueTime;
    private LocalDateTime shipmentDate;
    private String notes;
    private Long customerId;
    private String customerName;
    private String createdByUsername;
    private String carrierName;
    private String carrierVknTckn;
    private Set<EWaybillItemResponse> items;
    private LocalDateTime createdAt;
}