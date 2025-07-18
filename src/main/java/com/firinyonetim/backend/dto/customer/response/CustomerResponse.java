package com.firinyonetim.backend.dto.customer.response;

import com.firinyonetim.backend.dto.address.response.AddressResponse;
import com.firinyonetim.backend.dto.route.response.RouteResponse;
import com.firinyonetim.backend.dto.tax_info.response.TaxInfoResponse;
import com.firinyonetim.backend.entity.DayOfWeek;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

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
    private AddressResponse address; // YENİ
    private List<RouteResponse> routes; // Bu alanı eklediğinizi varsayıyorum

    private TaxInfoResponse taxInfo;

    private Set<DayOfWeek> workingDays;
    private Set<DayOfWeek> irsaliyeGunleri;
}