package com.firinyonetim.backend.dto.customer.response;

import com.firinyonetim.backend.dto.address.response.AddressResponse;
import com.firinyonetim.backend.dto.special_price.response.SpecialPriceResponse;
import com.firinyonetim.backend.dto.tax_info.response.TaxInfoResponse;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CustomerResponse {
    private Long id;
    private String name;
    private BigDecimal currentBalanceAmount;
    private String phone;
    private String email;
    private boolean isActive;
    private List<AddressResponse> addresses;
    private List<SpecialPriceResponse> specialPrices;

    private TaxInfoResponse taxInfo;
}