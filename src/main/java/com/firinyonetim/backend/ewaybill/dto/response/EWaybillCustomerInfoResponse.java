package com.firinyonetim.backend.ewaybill.dto.response;

import com.firinyonetim.backend.ewaybill.entity.EWaybillRecipientType;
import lombok.Data;

@Data
public class EWaybillCustomerInfoResponse {
    private Long customerId;
    private EWaybillRecipientType recipientType;
    private String defaultAlias;
}