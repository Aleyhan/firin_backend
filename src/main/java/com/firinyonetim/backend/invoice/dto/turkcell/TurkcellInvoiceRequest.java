package com.firinyonetim.backend.invoice.dto.turkcell;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TurkcellInvoiceRequest {
    private int recordType;
    private int status;
    private String localReferenceId;
    private boolean useManualInvoiceId;
    private String xsltCode;
    private String note;
    private AddressBook addressBook;
    private GeneralInfoModel generalInfoModel;
    private List<InvoiceLine> invoiceLines;

    @Data
    public static class AddressBook {
        private String name;
        private String identificationNumber;
        private String alias;
        private String receiverPersonSurName;
        private String receiverDistrict;
        private String receiverCity;
        private String receiverCountry;
        private String receiverEmail;
        private String receiverStreet;
        private String receiverBuildingName;
        private String receiverDoorNumber;
        private String receiverZipCode;
        private String receiverTaxOffice;
    }

    @Data
    public static class GeneralInfoModel {
        private int invoiceProfileType;
        private int type;
        private String issueDate;
        private String prefix;
        private String currencyCode;
        private BigDecimal exchangeRate;
    }

    @Data
    public static class InvoiceLine {
        private String inventoryCard;
        private BigDecimal amount;
        private String unitCode;
        private BigDecimal unitPrice;
        private String description;
        private BigDecimal discountAmount;
        private int vatRate;
        private BigDecimal vatAmount;
        private BigDecimal lineExtensionAmount;
    }
}