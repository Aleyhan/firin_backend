package com.firinyonetim.backend.invoice.mapper;

import com.firinyonetim.backend.entity.Customer;
import com.firinyonetim.backend.invoice.dto.turkcell.TurkcellInvoiceRequest;
import com.firinyonetim.backend.invoice.entity.Invoice;
import com.firinyonetim.backend.invoice.entity.InvoiceItem;
import com.firinyonetim.backend.invoice.entity.InvoiceSettings;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring")
public abstract class InvoiceTurkcellMapper {

    protected final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Mapping(target = "recordType", constant = "1")
    @Mapping(target = "status", constant = "20")
    @Mapping(target = "localReferenceId", source = "invoice.id")
    @Mapping(target = "useManualInvoiceId", constant = "false")
    @Mapping(target = "xsltCode", source = "settings.xsltCode")
    @Mapping(target = "note", source = "invoice.notes")
    @Mapping(target = "addressBook", expression = "java(mapAddressBook(invoice.getCustomer()))")
    @Mapping(target = "generalInfoModel", expression = "java(mapGeneralInfoModel(invoice, settings))")
    @Mapping(target = "invoiceLines", source = "invoice.items")
    public abstract TurkcellInvoiceRequest toTurkcellRequest(Invoice invoice, InvoiceSettings settings);

    @Mapping(source = "product.name", target = "inventoryCard")
    @Mapping(source = "quantity", target = "amount")
    @Mapping(source = "product.unit.code", target = "unitCode")
    @Mapping(source = "totalPrice", target = "lineExtensionAmount")
    public abstract TurkcellInvoiceRequest.InvoiceLine toInvoiceLine(InvoiceItem item);

    protected TurkcellInvoiceRequest.AddressBook mapAddressBook(Customer customer) {
        TurkcellInvoiceRequest.AddressBook addressBook = new TurkcellInvoiceRequest.AddressBook();
        addressBook.setName(customer.getName());
        addressBook.setIdentificationNumber(customer.getTaxInfo() != null ? customer.getTaxInfo().getTaxNumber() : null);
        addressBook.setReceiverCity(customer.getAddress() != null ? customer.getAddress().getProvince() : null);
        addressBook.setReceiverDistrict(customer.getAddress() != null ? customer.getAddress().getDistrict() : null);
        addressBook.setReceiverCountry("Türkiye"); // Varsayılan
        addressBook.setReceiverEmail(customer.getEmail());
        addressBook.setReceiverStreet(customer.getAddress() != null ? customer.getAddress().getDetails() : null);
        addressBook.setReceiverZipCode(customer.getAddress() != null ? customer.getAddress().getZipcode() : null);
        addressBook.setReceiverTaxOffice(customer.getTaxInfo() != null ? customer.getTaxInfo().getTaxOffice() : null);
        return addressBook;
    }

    protected TurkcellInvoiceRequest.GeneralInfoModel mapGeneralInfoModel(Invoice invoice, InvoiceSettings settings) {
        TurkcellInvoiceRequest.GeneralInfoModel model = new TurkcellInvoiceRequest.GeneralInfoModel();
        model.setInvoiceProfileType(invoice.getProfileType().ordinal()); // Enum sırasına göre (TEMEL=0, TICARI=1...)
        model.setType(invoice.getType().ordinal() + 1); // Enum sırası 0'dan başlıyor, API 1'den
        model.setIssueDate(invoice.getIssueDate().format(dateFormatter));
        model.setPrefix(settings.getPrefix());
        model.setCurrencyCode(invoice.getCurrencyCode());
        return model;
    }
}