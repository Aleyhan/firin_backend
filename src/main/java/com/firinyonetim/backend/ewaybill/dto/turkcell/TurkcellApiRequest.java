package com.firinyonetim.backend.ewaybill.dto.turkcell;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class TurkcellApiRequest {
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

    @Data
    public static class GeneralInfo {
        private String issueDate;
        private String issueTime;
        private int despatchProfileType = 0;
        private String currencyCode = "TRY";
        @JsonProperty("DespatchType")
        private int despatchType = 1;
    }

    @Data
    public static class AddressBook {
        private String identificationNumber;
        private String alias;
        private String name;
        // HATA MESAJINA GÖRE GÜNCELLEME
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
    public static class DespatchShipmentInfo {
        private String shipmentSenderTitle;
        private String shipmentSenderTcknVkn;
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
    public static class DespatchLine {
        private String productName;
        private BigDecimal amount;
        private String unitCode;
        private BigDecimal unitPrice;
        private BigDecimal lineAmount;
    }

    @Data
    public static class OrderInfo {
        private String shipmentDate;
    }
}