package com.firinyonetim.backend.dto.company_directory;

import com.firinyonetim.backend.validation.ValidCompanyInfo;
import lombok.Data;

@Data
@ValidCompanyInfo // YENİ ANOTASYON
public class CompanyInfoDto {
    private String identificationNumber;
    private String name;
    private String personSurName;
    private String taxOffice;
    private String registerNumber;
    private String webSite;
    private String phoneNumber;
    private String faxNumber;
    private String streetName;
    private String buildingName;
    private String buildingNumber;
    private String doorNumber;
    private String smallTown;
    private String district;
    private String city;
    private String zipCode;
    private String countryName;
}