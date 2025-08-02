package com.firinyonetim.backend.dto.supplier.response;

import com.firinyonetim.backend.dto.address.response.AddressResponse;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class SupplierResponse {
    private Long id;
    private String supplierCode;
    private String name;
    private String contactPerson;
    private String phone;
    private String email;
    private BigDecimal currentBalanceAmount;
    private boolean isActive;
    private String notes;
    private AddressResponse address;
    private SupplierTaxInfoResponse taxInfo; // YENİ ALAN
}