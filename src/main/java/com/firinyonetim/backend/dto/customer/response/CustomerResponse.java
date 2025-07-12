package com.firinyonetim.backend.dto.customer.response;

import com.firinyonetim.backend.dto.address.response.AddressResponse;
import com.firinyonetim.backend.dto.route.response.RouteResponse;
import com.firinyonetim.backend.dto.special_price.response.SpecialPriceResponse;
import com.firinyonetim.backend.dto.tax_info.response.TaxInfoResponse;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CustomerResponse {
    private Long id;
    private String customerCode; // Yeni alan
    private String name;
    private String notes; // Yeni alan

    private BigDecimal currentBalanceAmount;
    private String phone;
    private String email;
    private boolean isActive;
    private List<AddressResponse> addresses;
    private List<SpecialPriceResponse> specialPrices;
    private List<RouteResponse> routes; // Bu alanı eklediğinizi varsayıyorum

    private TaxInfoResponse taxInfo;
}