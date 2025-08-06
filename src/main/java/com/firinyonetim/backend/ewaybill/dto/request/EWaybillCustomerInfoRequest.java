package com.firinyonetim.backend.ewaybill.dto.request;

import com.firinyonetim.backend.ewaybill.entity.EWaybillRecipientType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EWaybillCustomerInfoRequest {
    @NotNull
    private EWaybillRecipientType recipientType;
    private String defaultAlias;
}