package com.firinyonetim.backend.invoice.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.firinyonetim.backend.entity.Customer;
import com.firinyonetim.backend.ewaybill.entity.EWaybillCustomerInfo;
import com.firinyonetim.backend.ewaybill.entity.EWaybillRecipientType;
import com.firinyonetim.backend.ewaybill.repository.EWaybillCustomerInfoRepository;
import com.firinyonetim.backend.invoice.dto.EWaybillForInvoiceDto;
import com.firinyonetim.backend.invoice.dto.turkcell.TurkcellInvoiceRequest;
import com.firinyonetim.backend.invoice.entity.Invoice;
import com.firinyonetim.backend.invoice.entity.InvoiceItem;
import com.firinyonetim.backend.invoice.entity.InvoiceSettings;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Mapper(componentModel = "spring")
public abstract class InvoiceTurkcellMapper {

    private EWaybillCustomerInfoRepository eWaybillCustomerInfoRepository;
    private ObjectMapper objectMapper;

    @Autowired
    public void setEWaybillCustomerInfoRepository(EWaybillCustomerInfoRepository eWaybillCustomerInfoRepository) {
        this.eWaybillCustomerInfoRepository = eWaybillCustomerInfoRepository;
    }

    @Autowired
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    protected final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Mapping(target = "recordType", constant = "1")
    @Mapping(target = "status", constant = "20")
    @Mapping(target = "localReferenceId", source = "invoice.id")
    @Mapping(target = "useManualInvoiceId", constant = "false")
    @Mapping(target = "xsltCode", source = "settings.xsltCode")
    @Mapping(target = "note", source = "invoice.notes")
    @Mapping(target = "addressBook", expression = "java(mapAddressBook(invoice.getCustomer()))")
    @Mapping(target = "generalInfoModel", expression = "java(mapGeneralInfoModel(invoice, settings))")
    @Mapping(target = "invoiceLines", source = "invoice.items")
    // @Mapping(target = "paymentMeansModel", expression = "java(mapPaymentMeansModel(invoice, settings))")
    @Mapping(target = "relatedDespatchList", expression = "java(mapRelatedDespatchList(invoice.getRelatedDespatchesJson()))")
    public abstract TurkcellInvoiceRequest toTurkcellRequest(Invoice invoice, InvoiceSettings settings);

    @Mapping(source = "product.name", target = "inventoryCard")
    @Mapping(source = "quantity", target = "amount")
    @Mapping(source = "product.unit.code", target = "unitCode")
    @Mapping(source = "totalPrice", target = "lineExtensionAmount")
    public abstract TurkcellInvoiceRequest.InvoiceLine toInvoiceLine(InvoiceItem item);

    protected TurkcellInvoiceRequest.PaymentMeansModel mapPaymentMeansModel(Invoice invoice, InvoiceSettings settings) {
        if (settings.getPaymentMeansCode() == null) {
            return null;
        }

        TurkcellInvoiceRequest.PaymentMeansModel model = new TurkcellInvoiceRequest.PaymentMeansModel();
        model.setPaymentMeansCode(settings.getPaymentMeansCode());
        model.setPaymentChannelCode(settings.getPaymentChannelCode());
        model.setInstructionNote(settings.getInstructionNote());
        model.setPayeeFinancialAccountId(settings.getPayeeFinancialAccountId());
        model.setPayeeFinancialAccountCurrencyCode(settings.getPayeeFinancialAccountCurrencyCode());


        return model;
    }

    protected List<TurkcellInvoiceRequest.RelatedDespatch> mapRelatedDespatchList(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            List<EWaybillForInvoiceDto> dtos = objectMapper.readValue(json, new TypeReference<>() {});
            return dtos.stream().map(dto -> {
                TurkcellInvoiceRequest.RelatedDespatch despatch = new TurkcellInvoiceRequest.RelatedDespatch();
                despatch.setDespatchNumber(dto.getDespatchNumber());
                despatch.setIssueDate(dto.getIssueDate().format(dateTimeFormatter));
                return despatch;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Could not parse relatedDespatchesJson: {}", json, e);
            return Collections.emptyList();
        }
    }

    protected TurkcellInvoiceRequest.AddressBook mapAddressBook(Customer customer) {
        EWaybillCustomerInfo customerInfo = eWaybillCustomerInfoRepository.findById(customer.getId())
                .orElseThrow(() -> new IllegalStateException("Müşteri '" + customer.getName() + "' için e-Fatura/e-İrsaliye bilgisi yapılandırılmamış."));

        TurkcellInvoiceRequest.AddressBook addressBook = new TurkcellInvoiceRequest.AddressBook();
        addressBook.setName(customer.getName());

        // Müşteri e-fatura mükellefi ise alias'ını, değilse e-arşiv için varsayılan alias'ı kullan
        if (customerInfo.getRecipientType() == EWaybillRecipientType.REGISTERED_USER) {
            addressBook.setAlias(customerInfo.getDefaultAlias());
        } else {
            // e-Arşiv faturalar için alias "defaultpk" veya "defaultgb" olabilir,
            // Turkcell dokümantasyonuna göre genellikle boş bırakılır veya özel bir değer atanır.
            // Şimdilik boş bırakmak en güvenlisi.
            addressBook.setAlias(null);
        }

        addressBook.setIdentificationNumber(customer.getTaxInfo() != null ? customer.getTaxInfo().getTaxNumber() : null);
        addressBook.setReceiverCity(customer.getAddress() != null ? customer.getAddress().getProvince() : null);
        addressBook.setReceiverDistrict(customer.getAddress() != null ? customer.getAddress().getDistrict() : null);
        addressBook.setReceiverCountry("Türkiye");
        addressBook.setReceiverEmail(customer.getEmail());
        addressBook.setReceiverStreet(customer.getAddress() != null ? customer.getAddress().getDetails() : null);
        addressBook.setReceiverZipCode(customer.getAddress() != null ? customer.getAddress().getZipcode() : null);
        addressBook.setReceiverTaxOffice(customer.getTaxInfo() != null ? customer.getTaxInfo().getTaxOffice() : null);
        return addressBook;
    }

    protected TurkcellInvoiceRequest.GeneralInfoModel mapGeneralInfoModel(Invoice invoice, InvoiceSettings settings) {
        TurkcellInvoiceRequest.GeneralInfoModel model = new TurkcellInvoiceRequest.GeneralInfoModel();
        model.setInvoiceProfileType(invoice.getProfileType().ordinal());
        model.setType(invoice.getType().ordinal() + 1);
        model.setIssueDate(invoice.getIssueDate().format(dateTimeFormatter));
        model.setPrefix(settings.getPrefix());
        model.setCurrencyCode(invoice.getCurrencyCode());
        return model;
    }
}