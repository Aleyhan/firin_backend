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
import java.util.ArrayList;
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
    @Mapping(target = "note", source = "invoice.notes") // NOT ARTIK DOĞRUDAN GELİYOR
    @Mapping(target = "addressBook", expression = "java(mapAddressBook(invoice.getCustomer()))")
    @Mapping(target = "generalInfoModel", expression = "java(mapGeneralInfoModel(invoice, settings))")
    @Mapping(target = "invoiceLines", expression = "java(mapInvoiceLines(invoice, settings))")
    @Mapping(target = "paymentMeansModel", expression = "java(mapPaymentMeansModel(invoice, settings))")
    @Mapping(target = "relatedDespatchList", expression = "java(mapRelatedDespatchList(invoice.getRelatedDespatchesJson()))")
    @Mapping(target = "ublSettingsModel", expression = "java(mapUblSettingsModel(settings))") // YENİ MAPPING
    public abstract TurkcellInvoiceRequest toTurkcellRequest(Invoice invoice, InvoiceSettings settings);

    // YENİ METOT
    protected TurkcellInvoiceRequest.UblSettingsModel mapUblSettingsModel(InvoiceSettings settings) {
        TurkcellInvoiceRequest.UblSettingsModel model = new TurkcellInvoiceRequest.UblSettingsModel();
        model.setUseCalculatedVatAmount(settings.getUseCalculatedVatAmount());
        model.setUseCalculatedTotalSummary(settings.getUseCalculatedTotalSummary());
        model.setHideDespatchMessage(settings.getHideDespatchMessage());
        return model;
    }

    protected List<TurkcellInvoiceRequest.InvoiceLine> mapInvoiceLines(Invoice invoice, InvoiceSettings settings) {
        List<TurkcellInvoiceRequest.InvoiceLine> lines = new ArrayList<>();
        for (InvoiceItem item : invoice.getItems()) {
            TurkcellInvoiceRequest.InvoiceLine line = new TurkcellInvoiceRequest.InvoiceLine();
            line.setInventoryCard(item.getProduct().getName());
            line.setAmount(new java.math.BigDecimal(item.getQuantity()));
            if(item.getProduct().getUnit() != null && item.getProduct().getUnit().getCode() != null) {
                line.setUnitCode(item.getProduct().getUnit().getCode());
            } else {
                line.setUnitCode("C62");
            }
            line.setUnitPrice(item.getUnitPrice());
            line.setDescription(item.getDescription());
            line.setDiscountAmount(item.getDiscountAmount());
            line.setVatRate(item.getVatRate());

            if (Boolean.TRUE.equals(settings.getUseCalculatedVatAmount())) {
                line.setLineExtensionAmount(item.getTotalPrice());
                line.setVatAmount(item.getVatAmount());
            }

            lines.add(line);
        }
        return lines;
    }

    protected TurkcellInvoiceRequest.PaymentMeansModel mapPaymentMeansModel(Invoice invoice, InvoiceSettings settings) {
        if (!StringUtils.hasText(settings.getPaymentMeansCode())) {
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

        if (customerInfo.getRecipientType() == EWaybillRecipientType.REGISTERED_USER) {
            addressBook.setAlias(customerInfo.getDefaultAlias());
        } else {
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