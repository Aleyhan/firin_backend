package com.firinyonetim.backend.ewaybill.dto.turkcell;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TurkcellApiRequest {
    private int status = 20;
    @JsonProperty("isNew")
    private boolean isNew = true;
    public boolean getIsNew() { return this.isNew; }
    public void setIsNew(boolean isNew) { this.isNew = isNew; }
    private boolean useManualDespatchAdviceId = false;

    @JsonProperty("localReferenceId")
    private String localReferenceId;

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

    private List<NoteLine> notes;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class GeneralInfo {
        private String issueDate;
        private String issueTime;
        private int despatchProfileType = 0;
        private String currencyCode = "TRY";
        @JsonProperty("DespatchType")
        private int despatchType = 1;
        private String prefix = "KEI";
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class NoteLine {
        private String note;
    }


    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DespatchLine {
        private String productName;
        private java.math.BigDecimal amount;
        private String unitCode;
        @JsonProperty("manufacturersItemIdentification")
        private String manufacturersItemIdentification;
    }

    // DEĞİŞİKLİK BURADA BAŞLIYOR: AddressBook detaylandırıldı
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AddressBook {
        private String identificationNumber;
        private String alias;
        private String name;
        private String receiverPersonSurName; // YENİ ALAN
        private String receiverStreet;
        private String receiverDistrict;
        private String receiverCity;
        private String receiverCountry;
        private String receiverPhoneNumber;
        private String receiverEmail;
        private String receiverTaxOffice;
        private String receiverZipCode;
    }
    // DEĞİŞİKLİK BURADA BİTİYOR

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DeliveryAddressInfo {
        @JsonProperty("street")
        private String street;
        @JsonProperty("district")
        private String district;
        @JsonProperty("city")
        private String city;
        @JsonProperty("zipCode")
        private String zipCode;
        @JsonProperty("countryName")
        private String countryName;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DespatchShipmentInfo {
        private String shipmentPlateNo;
        private String shipmentSenderTitle;
        private String shipmentSenderTcknVkn;
        private List<DriverLine> driverLines;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DriverLine {
        private String driverName;
        private String driverSurname;
        private String driverTckn;
    }

    // DEĞİŞİKLİK BURADA BAŞLIYOR: despatchBuyerCustomerInfo sadeleştirildi
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DespatchBuyerCustomerInfo {
        private String identificationNumber;
        private String name;
        private String personSurName; // YENİ ALAN
        private String countryName;
        private String city;
        private String district;
    }
    // DEĞİŞİKLİK BURADA BİTİYOR

    @Data
    public static class SellerSupplierInfo {
        private String identificationNumber;
        private String name;
        @JsonProperty("PersonSurName")
        private String personSurName;
        private String countryName;
        private String city;
        private String district;
    }
    @Data
    public static class OrderInfo {
        private String shipmentDate;
    }
}