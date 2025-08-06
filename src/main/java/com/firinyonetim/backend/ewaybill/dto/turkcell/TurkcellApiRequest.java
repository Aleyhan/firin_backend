package com.firinyonetim.backend.ewaybill.dto.turkcell;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class TurkcellApiRequest {
    // ... (status, isNew, useManualDespatchAdviceId alanları aynı)
    private int status = 20;
    @JsonProperty("isNew")
    private boolean isNew = true;
    public boolean getIsNew() { return this.isNew; }
    public void setIsNew(boolean isNew) { this.isNew = isNew; }
    private boolean useManualDespatchAdviceId = false;

    @JsonProperty("generalInfo")
    private GeneralInfo generalInfo;

    @JsonProperty("addressBook")
    private AddressBook addressBook;

    @JsonProperty("deliveryAddressInfo")
    private DeliveryAddressInfo deliveryAddressInfo;

    @JsonProperty("despatchShipmentInfo")
    private DespatchShipmentInfo despatchShipmentInfo;

    @JsonProperty("despatchBuyerCustomerInfo")
    private DespatchBuyerCustomerInfo despatchBuyerCustomerInfo;

    @JsonProperty("sellerSupplierInfo")
    private SellerSupplierInfo sellerSupplierInfo;

    @JsonProperty("despatchLines")
    private List<DespatchLine> despatchLines;

    @JsonProperty("orderInfo")
    private OrderInfo orderInfo;

    // YENİ: Genel Toplamlar için TaxTotal nesnesi
    @JsonProperty("taxTotal")
    private TaxTotal taxTotal;

    @Data
    public static class GeneralInfo {
        // ... (diğer alanlar aynı)
        private String issueDate;
        private String issueTime;
        private int despatchProfileType = 0;
        private String currencyCode = "TRY";
        @JsonProperty("DespatchType")
        private int despatchType = 1;
        // YENİ: KDV Dahil Toplam Tutar
        @JsonProperty("PayableAmount")
        private BigDecimal payableAmount;
        private BigDecimal totalAmount;

    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL) // İç sınıflar için de eklemek iyi bir pratik
    public static class DespatchLine {
        private String productName;
        private BigDecimal amount;
        private String unitCode;
        private BigDecimal unitPrice; // KDV Hariç
        private BigDecimal lineAmount; // KDV Hariç
//        private String description;

        @JsonProperty("taxTotal")
        private TaxTotal lineTaxTotal;

        // DÜZENLEME: Sadece üretici ürün kodunu ekliyoruz.
        @JsonProperty("manufacturersItemIdentification")
        private String manufacturersItemIdentification;
    }


    // YENİ: Hem kalem hem de genel toplam için kullanılacak ortak KDV nesnesi
    @Data
    public static class TaxTotal {
        @JsonProperty("taxAmount")
        private BigDecimal taxAmount;
        @JsonProperty("taxSubTotals")
        private List<TaxSubTotal> taxSubTotals;
    }

    // YENİ: Farklı KDV oranlarını gruplamak için
    @Data
    public static class TaxSubTotal {
        @JsonProperty("taxableAmount")
        private BigDecimal taxableAmount; // KDV Matrahı
        @JsonProperty("taxAmount")
        private BigDecimal taxAmount; // KDV Tutarı
        @JsonProperty("percent")
        private int percent; // KDV Oranı
    }

    // ... (diğer iç sınıflar aynı kalacak)
    @Data
    public static class AddressBook {
        private String identificationNumber;
        private String alias;
        private String name;
        @JsonProperty("ReceiverCity")
        private String receiverCity;
        @JsonProperty("ReceiverCountry")
        private String receiverCountry;
        @JsonProperty("ReceiverDistrict")
        private String receiverDistrict;
    }
    @Data
    public static class DeliveryAddressInfo {
        @JsonProperty("District")
        private String district;
        @JsonProperty("City")
        private String city;
        @JsonProperty("ZipCode")
        private String zipCode;
        @JsonProperty("CountryName")
        private String countryName;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DespatchShipmentInfo {
        // Bu alanlar artık her zaman gönderici (bizim fırın) bilgisi olacak
        private String shipmentSenderTitle;
        private String shipmentSenderTcknVkn;

        // Plaka bilgisi
        private String shipmentPlateNo;

        // Şoför bilgileri artık bu dizinin içinde olacak
        @JsonProperty("driverLines")
        private List<DriverLine> driverLines;
    }

    // YENİ İÇ SINIF: Şoför bilgilerini temsil eder
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DriverLine {
        private String driverName;
        private String driverSurname;
        private String driverTckn;
    }

    @Data
    public static class DespatchBuyerCustomerInfo {
        private String identificationNumber;
        private String name;
        @JsonProperty("City")
        private String city;
        @JsonProperty("District")
        private String district;
        @JsonProperty("CountryName")
        private String countryName;
    }
    @Data
    public static class SellerSupplierInfo {
        private String identificationNumber;
        private String name;
        @JsonProperty("PersonSurName")
        private String personSurName;
        @JsonProperty("City")
        private String city;
        @JsonProperty("District")
        private String district;
        @JsonProperty("CountryName")
        private String countryName;
    }
    @Data
    public static class OrderInfo {
        private String shipmentDate;
    }
}