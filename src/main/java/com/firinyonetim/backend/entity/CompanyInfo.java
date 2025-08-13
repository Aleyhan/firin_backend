package com.firinyonetim.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "company_info")
public class CompanyInfo {

    @Id
    private Long id;

    // Temel Kimlik
    @Column(nullable = false)
    private String identificationNumber; // VKN/TCKN

    @Column(nullable = false)
    private String name; // Şahıs ise Ad, Şirket ise Ticari Unvan

    private String personSurName; // Şahıs ise Soyad

    private String taxOffice;

    private String registerNumber; // Ticaret Sicil No

    private String webSite;

    // İletişim
    private String phoneNumber;
    private String faxNumber;

    // Adres
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