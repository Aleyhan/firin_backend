package com.firinyonetim.backend.ewaybill.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.firinyonetim.backend.entity.Address;
import com.firinyonetim.backend.entity.Customer;
import com.firinyonetim.backend.entity.Product;
import com.firinyonetim.backend.entity.Route;
import com.firinyonetim.backend.entity.TaxInfo;
import com.firinyonetim.backend.entity.User;
import com.firinyonetim.backend.ewaybill.dto.request.EWaybillCreateRequest;
import com.firinyonetim.backend.ewaybill.dto.request.EWaybillItemRequest;
import com.firinyonetim.backend.ewaybill.dto.response.EWaybillResponse;
import com.firinyonetim.backend.ewaybill.dto.turkcell.TurkcellApiRequest;
import com.firinyonetim.backend.ewaybill.dto.turkcell.TurkcellApiResponse;
import com.firinyonetim.backend.ewaybill.entity.*;
import com.firinyonetim.backend.ewaybill.mapper.EWaybillMapper;
import com.firinyonetim.backend.ewaybill.repository.EWaybillCustomerInfoRepository;
import com.firinyonetim.backend.ewaybill.repository.EWaybillRepository;
import com.firinyonetim.backend.exception.ResourceNotFoundException;
import com.firinyonetim.backend.repository.CustomerRepository;
import com.firinyonetim.backend.repository.ProductRepository;
import com.firinyonetim.backend.repository.RouteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;

import com.firinyonetim.backend.entity.DayOfWeek; // YENİ IMPORT
import java.time.format.TextStyle; // YENİ IMPORT
import java.util.Locale; // YENİ IMPORT
import java.util.Set; // YENİ IMPORT

import com.firinyonetim.backend.ewaybill.dto.request.BulkEWaybillFromTemplateRequest; // YENİ
import com.firinyonetim.backend.ewaybill.entity.EWaybillTemplate; // YENİ
import com.firinyonetim.backend.ewaybill.repository.EWaybillTemplateRepository; // YENİ
import java.util.ArrayList; // YENİ
import java.util.List; // YENİ

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EWaybillService {

    private final EWaybillRepository eWaybillRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final RouteRepository routeRepository;
    private final TurkcellEWaybillClient turkcellClient;
    private final EWaybillMapper eWaybillMapper;
    private final ObjectMapper objectMapper;
    private final EWaybillCustomerInfoRepository eWaybillCustomerInfoRepository;
    private final EWaybillTemplateRepository eWaybillTemplateRepository;

    @Value("${ewaybill.sender.vkn}")
    private String senderVkn;
    @Value("${ewaybill.sender.name}")
    private String senderName;
    @Value("${ewaybill.sender.city}")
    private String senderCity;
    @Value("${ewaybill.sender.district}")
    private String senderDistrict;
    @Value("${ewaybill.sender.country}")
    private String senderCountry;

    @Transactional(readOnly = true)
    public List<EWaybillResponse> findAll() {
        return eWaybillRepository.findAll().stream()
                .map(eWaybillMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EWaybillResponse findById(UUID id) {
        EWaybill ewaybill = eWaybillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("E-Waybill not found with id: " + id));
        return eWaybillMapper.toResponseDto(ewaybill);
    }

    @Transactional
    public EWaybillResponse createEWaybill(EWaybillCreateRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + request.getCustomerId()));

        validateIssueDate(customer, request.getIssueDate());

        EWaybill ewaybill = eWaybillMapper.fromCreateRequest(request);
        ewaybill.setCreatedBy(currentUser);
        ewaybill.setCustomer(customer);

        // DÜZELTME: Rota ID'si geldiyse, rotayı bul ve plakasını set et
        if (request.getRouteId() != null) {
            Route route = routeRepository.findById(request.getRouteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + request.getRouteId()));
            ewaybill.setPlateNumber(route.getPlaka());
            log.info("Route found for e-waybill: {}, Plate number set to: {}", route.getId(), route.getPlaka());
        }
        try {
            ewaybill.setDeliveryAddressJson(objectMapper.writeValueAsString(customer.getAddress()));
        } catch (JsonProcessingException e) {
            log.error("Could not serialize delivery address for customer {}", customer.getId(), e);
            throw new RuntimeException("Delivery address could not be processed.");
        }

        Set<EWaybillItem> items = processItems(request.getItems(), ewaybill);
        ewaybill.setItems(items);

        calculateAndSetTotals(ewaybill);

        EWaybill savedEWaybill = eWaybillRepository.save(ewaybill);
        return eWaybillMapper.toResponseDto(savedEWaybill);
    }

    @Transactional
    public EWaybillResponse updateEWaybill(UUID id, EWaybillCreateRequest request) {
        EWaybill ewaybill = eWaybillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("E-Waybill not found with id: " + id));

        if (ewaybill.getStatus() != EWaybillStatus.DRAFT && ewaybill.getStatus() != EWaybillStatus.API_ERROR) {
            throw new IllegalStateException("Only e-waybills in DRAFT or API_ERROR status can be updated.");
        }

        validateIssueDate(ewaybill.getCustomer(), request.getIssueDate());


        eWaybillMapper.updateFromRequest(request, ewaybill);

        // DÜZELTME: Rota ID'si geldiyse, rotayı bul ve plakasını set et
        if (request.getRouteId() != null) {
            Route route = routeRepository.findById(request.getRouteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + request.getRouteId()));
            ewaybill.setPlateNumber(route.getPlaka());
        } else {
            ewaybill.setPlateNumber(null); // Rota kaldırıldıysa plakayı da temizle
        }

        ewaybill.getItems().clear();
        Set<EWaybillItem> updatedItems = processItems(request.getItems(), ewaybill);
        ewaybill.getItems().addAll(updatedItems);

        calculateAndSetTotals(ewaybill);

        EWaybill updated = eWaybillRepository.save(ewaybill);
        return eWaybillMapper.toResponseDto(updated);
    }

    @Transactional
    public void deleteEWaybill(UUID id) {
        EWaybill ewaybill = eWaybillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("E-Waybill not found with id: " + id));

        if (ewaybill.getStatus() != EWaybillStatus.DRAFT && ewaybill.getStatus() != EWaybillStatus.API_ERROR) {
            throw new IllegalStateException("Only e-waybills in DRAFT or API_ERROR status can be deleted.");
        }
        eWaybillRepository.delete(ewaybill);
    }

    private Set<EWaybillItem> processItems(Set<EWaybillItemRequest> itemRequests, EWaybill ewaybill) {
        Set<EWaybillItem> items = new HashSet<>();
        for (EWaybillItemRequest itemDto : itemRequests) {
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + itemDto.getProductId()));

            EWaybillItem item = new EWaybillItem();
            item.setEWaybill(ewaybill);
            item.setProduct(product);
            item.setProductNameSnapshot(product.getName());
            item.setQuantity(itemDto.getQuantity());

            BigDecimal unitPrice = itemDto.getUnitPrice() != null ? itemDto.getUnitPrice() : BigDecimal.ZERO;
            item.setUnitPrice(unitPrice);

            String unitCode = "C62";
            if (product.getUnit() != null && StringUtils.hasText(product.getUnit().getCode())) {
                unitCode = product.getUnit().getCode();
            }
            item.setUnitCode(unitCode);

            BigDecimal lineAmount = itemDto.getQuantity().multiply(unitPrice);
            item.setLineAmount(lineAmount.setScale(2, RoundingMode.HALF_UP));

            item.setVatRate(product.getVatRate());
            BigDecimal vatAmount = lineAmount.multiply(BigDecimal.valueOf(product.getVatRate())).divide(new BigDecimal(100));
            item.setVatAmount(vatAmount.setScale(2, RoundingMode.HALF_UP));

            items.add(item);
        }
        return items;
    }

    private void calculateAndSetTotals(EWaybill ewaybill) {
        BigDecimal totalAmountWithoutVat = BigDecimal.ZERO;
        BigDecimal totalVatAmount = BigDecimal.ZERO;

        for (EWaybillItem item : ewaybill.getItems()) {
            totalAmountWithoutVat = totalAmountWithoutVat.add(item.getLineAmount());
            totalVatAmount = totalVatAmount.add(item.getVatAmount());
        }

        ewaybill.setTotalAmountWithoutVat(totalAmountWithoutVat.setScale(2, RoundingMode.HALF_UP));
        ewaybill.setTotalVatAmount(totalVatAmount.setScale(2, RoundingMode.HALF_UP));
        ewaybill.setTotalAmountWithVat(totalAmountWithoutVat.add(totalVatAmount).setScale(2, RoundingMode.HALF_UP));
    }

    @Transactional
    public void sendEWaybill(UUID ewaybillId) {
        log.info("Sending e-waybill with internal id: {}", ewaybillId);
        EWaybill ewaybill = eWaybillRepository.findById(ewaybillId)
                .orElseThrow(() -> new ResourceNotFoundException("EWaybill not found with id: " + ewaybillId));

        if (ewaybill.getStatus() != EWaybillStatus.DRAFT && ewaybill.getStatus() != EWaybillStatus.API_ERROR) {
            throw new IllegalStateException("Only e-waybills in DRAFT or API_ERROR status can be sent.");
        }

        TurkcellApiRequest request = buildTurkcellRequest(ewaybill);

        try {
            TurkcellApiResponse response = turkcellClient.createEWaybill(request);

            ewaybill.setTurkcellApiId(response.getId());
            ewaybill.setEwaybillNumber(response.getDespatchAdviceNumber() != null ? response.getDespatchAdviceNumber() : response.getDespatchNumber());
            ewaybill.setStatus(EWaybillStatus.SENDING);
            ewaybill.setTurkcellStatus(response.getStatus());
            ewaybill.setStatusMessage("Successfully queued for sending.");

            eWaybillRepository.save(ewaybill);
            log.info("E-waybill {} successfully sent to Turkcell API. Turkcell ID: {}", ewaybillId, response.getId());

        } catch (HttpClientErrorException e) {
            String responseBody = e.getResponseBodyAsString();
            log.error("Error sending e-waybill {} to Turkcell API. Status: {}, Body: {}", ewaybillId, e.getStatusCode(), responseBody);

            ewaybill.setStatus(EWaybillStatus.API_ERROR);
            ewaybill.setStatusMessage(responseBody);
            eWaybillRepository.save(ewaybill);

            throw new RuntimeException(responseBody, e);

        } catch (Exception e) {
            log.error("Generic error sending e-waybill {} to Turkcell API: {}", ewaybillId, e.getMessage());

            ewaybill.setStatus(EWaybillStatus.API_ERROR);
            ewaybill.setStatusMessage("Failed to send to Turkcell API: " + e.getMessage());
            eWaybillRepository.save(ewaybill);

            throw new RuntimeException("Failed to send e-waybill to Turkcell API.", e);
        }
    }

    private TurkcellApiRequest buildTurkcellRequest(EWaybill ewaybill) {
        TurkcellApiRequest request = eWaybillMapper.toTurkcellApiRequest(ewaybill);
        Customer customer = ewaybill.getCustomer();
        Address address = customer.getAddress();
        TaxInfo taxInfo = customer.getTaxInfo();

        if (taxInfo == null) throw new IllegalStateException("Customer tax info is required to send an e-waybill.");
        if (address == null) throw new IllegalStateException("Customer address is required to send an e-waybill.");


        // DİNAMİK ALIAS MANTIĞI
        EWaybillCustomerInfo customerInfo = eWaybillCustomerInfoRepository.findById(customer.getId())
                .orElseThrow(() -> new IllegalStateException("E-Waybill info for customer " + customer.getName() + " is not configured."));

        String targetAlias;
        if (customerInfo.getRecipientType() == EWaybillRecipientType.REGISTERED_USER) {
            targetAlias = customerInfo.getDefaultAlias();
        } else {
            targetAlias = "urn:mail:irsaliyepk@gib.gov.tr";
        }

        TurkcellApiRequest.AddressBook addressBook = new TurkcellApiRequest.AddressBook();
        addressBook.setIdentificationNumber(taxInfo.getTaxNumber());
        addressBook.setName(customer.getName());
        addressBook.setAlias(targetAlias); // DİNAMİK DEĞERİ ATA


        addressBook.setReceiverCity(address.getProvince());
        addressBook.setReceiverDistrict(address.getDistrict());
        addressBook.setReceiverCountry("Türkiye");
        request.setAddressBook(addressBook);

        TurkcellApiRequest.DeliveryAddressInfo deliveryAddress = new TurkcellApiRequest.DeliveryAddressInfo();
        deliveryAddress.setCity(address.getProvince());
        deliveryAddress.setDistrict(address.getDistrict());
        deliveryAddress.setCountryName("Türkiye");
        deliveryAddress.setZipCode("34000");
        request.setDeliveryAddressInfo(deliveryAddress);

        TurkcellApiRequest.DespatchBuyerCustomerInfo buyerInfo = new TurkcellApiRequest.DespatchBuyerCustomerInfo();
        buyerInfo.setIdentificationNumber(taxInfo.getTaxNumber());
        buyerInfo.setName(customer.getName());
        buyerInfo.setCity(address.getProvince());
        buyerInfo.setDistrict(address.getDistrict());
        buyerInfo.setCountryName("Türkiye");
        request.setDespatchBuyerCustomerInfo(buyerInfo);

        TurkcellApiRequest.DespatchShipmentInfo shipmentInfo = new TurkcellApiRequest.DespatchShipmentInfo();
        String vknTckn = ewaybill.getCarrierVknTckn();
        String name = ewaybill.getCarrierName();

        shipmentInfo.setShipmentSenderTitle(senderName);
        shipmentInfo.setShipmentSenderTcknVkn(senderVkn);

        if (StringUtils.hasText(ewaybill.getPlateNumber())) {
            shipmentInfo.setShipmentPlateNo(ewaybill.getPlateNumber());
        }

        if (StringUtils.hasText(name) && StringUtils.hasText(vknTckn)) {
            TurkcellApiRequest.DriverLine driverLine = new TurkcellApiRequest.DriverLine();
            driverLine.setDriverTckn(vknTckn);

            int lastSpaceIndex = name.lastIndexOf(' ');
            if (lastSpaceIndex > 0 && (lastSpaceIndex < name.length() - 1)) {
                driverLine.setDriverName(name.substring(0, lastSpaceIndex));
                driverLine.setDriverSurname(name.substring(lastSpaceIndex + 1));
            } else {
                driverLine.setDriverName(name);
                driverLine.setDriverSurname("-");
            }
            shipmentInfo.setDriverLines(List.of(driverLine));
        }
        request.setDespatchShipmentInfo(shipmentInfo);

        TurkcellApiRequest.SellerSupplierInfo sellerInfo = new TurkcellApiRequest.SellerSupplierInfo();
        sellerInfo.setIdentificationNumber(senderVkn);
        sellerInfo.setName(senderName);
        sellerInfo.setPersonSurName("-");
        sellerInfo.setCity(senderCity);
        sellerInfo.setDistrict(senderDistrict);
        sellerInfo.setCountryName(senderCountry);
        request.setSellerSupplierInfo(sellerInfo);

        List<TurkcellApiRequest.DespatchLine> despatchLines = new ArrayList<>();
        for (EWaybillItem item : ewaybill.getItems()) {
            TurkcellApiRequest.DespatchLine line = new TurkcellApiRequest.DespatchLine();

            line.setProductName(item.getProductNameSnapshot());
            line.setAmount(item.getQuantity());
            line.setUnitCode(item.getUnitCode());

            if (item.getUnitPrice() != null && item.getUnitPrice().compareTo(BigDecimal.ZERO) > 0) {
                line.setUnitPrice(item.getUnitPrice());
                line.setLineAmount(item.getLineAmount());
                line.setManufacturersItemIdentification(item.getProduct().getId().toString());

                TurkcellApiRequest.TaxSubTotal lineTaxSubTotal = new TurkcellApiRequest.TaxSubTotal();
                lineTaxSubTotal.setTaxableAmount(item.getLineAmount());
                lineTaxSubTotal.setTaxAmount(item.getVatAmount());
                lineTaxSubTotal.setPercent(item.getVatRate());

                TurkcellApiRequest.TaxTotal lineTaxTotal = new TurkcellApiRequest.TaxTotal();
                lineTaxTotal.setTaxAmount(item.getVatAmount());
                lineTaxTotal.setTaxSubTotals(List.of(lineTaxSubTotal));

                line.setLineTaxTotal(lineTaxTotal);
            }

            despatchLines.add(line);
        }
        request.setDespatchLines(despatchLines);

        Map<Integer, BigDecimal> vatRateToTaxableAmountMap = ewaybill.getItems().stream()
                .collect(Collectors.groupingBy(
                        EWaybillItem::getVatRate,
                        Collectors.mapping(EWaybillItem::getLineAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));

        List<TurkcellApiRequest.TaxSubTotal> taxSubTotals = new ArrayList<>();
        for (Map.Entry<Integer, BigDecimal> entry : vatRateToTaxableAmountMap.entrySet()) {
            Integer vatRate = entry.getKey();
            BigDecimal taxableAmount = entry.getValue();
            BigDecimal taxAmount = taxableAmount.multiply(BigDecimal.valueOf(vatRate)).divide(new BigDecimal(100));

            TurkcellApiRequest.TaxSubTotal subTotal = new TurkcellApiRequest.TaxSubTotal();
            subTotal.setPercent(vatRate);
            subTotal.setTaxableAmount(taxableAmount.setScale(2, RoundingMode.HALF_UP));
            subTotal.setTaxAmount(taxAmount.setScale(2, RoundingMode.HALF_UP));
            taxSubTotals.add(subTotal);
        }

        TurkcellApiRequest.TaxTotal totalTax = new TurkcellApiRequest.TaxTotal();
        totalTax.setTaxAmount(ewaybill.getTotalVatAmount());
        totalTax.setTaxSubTotals(taxSubTotals);
        request.setTaxTotal(totalTax);

        request.getGeneralInfo().setPayableAmount(ewaybill.getTotalAmountWithVat());
        request.getGeneralInfo().setTotalAmount(ewaybill.getTotalAmountWithoutVat());

        return request;
    }

    @Transactional
    public void checkAndUpdateStatuses() {
        List<EWaybill> ewaybillsToCheck = eWaybillRepository.findEWaybillsToQueryStatus();
        if (ewaybillsToCheck.isEmpty()) {
            return;
        }
        log.info("Found {} e-waybills with SENDING or AWAITING_APPROVAL status to check.", ewaybillsToCheck.size());

        for (EWaybill ewaybill : ewaybillsToCheck) {
            if (ewaybill.getTurkcellApiId() == null) {
                log.warn("E-waybill with internal id {} has a status to be checked but no Turkcell API ID. Skipping.", ewaybill.getId());
                continue;
            }

            try {
                TurkcellApiResponse response = turkcellClient.getEWaybillStatus(ewaybill.getTurkcellApiId());
                updateStatusFromResponse(ewaybill, response);
                eWaybillRepository.save(ewaybill);
            } catch (Exception e) {
                log.error("Failed to check status for e-waybill with Turkcell ID {}: {}", ewaybill.getTurkcellApiId(), e.getMessage());
            }
        }
    }

    private void updateStatusFromResponse(EWaybill ewaybill, TurkcellApiResponse response) {
        ewaybill.setTurkcellStatus(response.getStatus());
        ewaybill.setStatusMessage(response.getMessage());

        switch (response.getStatus()) {
            case 70:
                ewaybill.setStatus(EWaybillStatus.AWAITING_APPROVAL);
                break;
            case 60:
                ewaybill.setStatus(EWaybillStatus.APPROVED);
                break;
            case 40:
                ewaybill.setStatus(EWaybillStatus.REJECTED_BY_GIB);
                break;
            case 80:
                ewaybill.setStatus(EWaybillStatus.REJECTED_BY_RECIPIENT);
                break;
            default:
                break;
        }
        log.info("Updated status for e-waybill {}. New Turkcell status: {}, New internal status: {}",
                ewaybill.getId(), ewaybill.getTurkcellStatus(), ewaybill.getStatus());
    }

    @Transactional(readOnly = true)
    public byte[] getEWaybillPdf(UUID id) {
        EWaybill ewaybill = eWaybillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("E-Waybill not found with id: " + id));
        if (ewaybill.getTurkcellApiId() == null) {
            throw new IllegalStateException("This e-waybill has not been sent to the provider yet.");
        }
        return turkcellClient.getEWaybillAsPdf(ewaybill.getTurkcellApiId());
    }

    @Transactional(readOnly = true)
    public String getEWaybillHtml(UUID id) {
        EWaybill ewaybill = eWaybillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("E-Waybill not found with id: " + id));
        if (ewaybill.getTurkcellApiId() == null) {
            throw new IllegalStateException("This e-waybill has not been sent to the provider yet.");
        }
        return turkcellClient.getEWaybillAsHtml(ewaybill.getTurkcellApiId());
    }

    // YENİ YARDIMCI METOT
    private void validateIssueDate(Customer customer, java.time.LocalDate issueDate) {
        Set<DayOfWeek> irsaliyeGunleri = customer.getIrsaliyeGunleri();
        if (irsaliyeGunleri != null && !irsaliyeGunleri.isEmpty()) {
            java.time.DayOfWeek dayOfWeek = issueDate.getDayOfWeek(); // Java'nın kendi DayOfWeek enum'ı

            // Java'nın DayOfWeek'ını bizim kendi DayOfWeek enum'ımıza çevir
            DayOfWeek bizimDayOfWeek;
            try {
                bizimDayOfWeek = DayOfWeek.valueOf(dayOfWeek.name());
            } catch (IllegalArgumentException e) {
                // Bu durum normalde yaşanmaz ama güvenlik için
                throw new IllegalStateException("Invalid day of week: " + dayOfWeek.name());
            }

            if (!irsaliyeGunleri.contains(bizimDayOfWeek)) {
                String dayName = dayOfWeek.getDisplayName(TextStyle.FULL, new Locale("tr", "TR"));
                throw new IllegalArgumentException(
                        "İrsaliye oluşturma başarısız: Seçilen tarih (" + dayName + ") müşterinin irsaliye günlerinden biri değil."
                );
            }
        } else {
            // Eğer müşterinin irsaliye günü tanımlanmamışsa, hiçbir gün irsaliye kesilemez.
            throw new IllegalArgumentException(
                    "İrsaliye oluşturma başarısız: Bu müşteri için herhangi bir irsaliye günü tanımlanmamış."
            );
        }
    }

    // YENİ METOT
    @Transactional
    public List<EWaybillResponse> createEWaybillsFromTemplates(BulkEWaybillFromTemplateRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<EWaybillResponse> createdEWaybills = new ArrayList<>();

        for (Long customerId : request.getCustomerIds()) {
            Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));
            validateIssueDate(customer, request.getIssueDate());
        }

        for (Long customerId : request.getCustomerIds()) {
            Customer customer = customerRepository.findById(customerId).get();
            EWaybillTemplate template = eWaybillTemplateRepository.findByCustomerId(customerId)
                    .orElseThrow(() -> new IllegalStateException("Template not found for customer: " + customer.getName()));

            EWaybill ewaybill = new EWaybill();
            ewaybill.setCreatedBy(currentUser);
            ewaybill.setCustomer(customer);

            ewaybill.setIssueDate(request.getIssueDate());
            ewaybill.setIssueTime(request.getIssueTime());
            ewaybill.setShipmentDate(request.getShipmentDate());

            ewaybill.setNotes(template.getNotes());
            ewaybill.setCarrierName(template.getCarrierName());
            ewaybill.setCarrierVknTckn(template.getCarrierVknTckn());
            ewaybill.setPlateNumber(template.getPlateNumber());

            // --- YENİ MANTIK ---
            // Şablonda fiyat alanları dahil edilmemişse, tutarları 0 olarak ayarla.
            boolean includePrices = template.getIncludedFields() != null && template.getIncludedFields().contains("unitPrice");
            if (includePrices) {
                ewaybill.setTotalAmountWithoutVat(template.getTotalAmountWithoutVat());
                ewaybill.setTotalVatAmount(template.getTotalVatAmount());
                ewaybill.setTotalAmountWithVat(template.getTotalAmountWithVat());
            } else {
                ewaybill.setTotalAmountWithoutVat(BigDecimal.ZERO);
                ewaybill.setTotalVatAmount(BigDecimal.ZERO);
                ewaybill.setTotalAmountWithVat(BigDecimal.ZERO);
            }
            // --- YENİ MANTIK SONU ---

            template.getItems().forEach(templateItem -> {
                EWaybillItem newItem = new EWaybillItem();
                newItem.setEWaybill(ewaybill);
                newItem.setProduct(templateItem.getProduct());
                newItem.setProductNameSnapshot(templateItem.getProductNameSnapshot());
                newItem.setQuantity(templateItem.getQuantity());
                newItem.setUnitCode(templateItem.getUnitCode());

                // --- YENİ MANTIK ---
                if (includePrices) {
                    newItem.setUnitPrice(templateItem.getUnitPrice());
                    newItem.setLineAmount(templateItem.getLineAmount());
                    newItem.setVatRate(templateItem.getVatRate());
                    newItem.setVatAmount(templateItem.getVatAmount());
                } else {
                    newItem.setUnitPrice(BigDecimal.ZERO);
                    newItem.setLineAmount(BigDecimal.ZERO);
                    newItem.setVatRate(templateItem.getVatRate()); // KDV oranı kalabilir, tutar 0 olacak
                    newItem.setVatAmount(BigDecimal.ZERO);
                }
                // --- YENİ MANTIK SONU ---
                ewaybill.getItems().add(newItem);
            });

            EWaybill savedEWaybill = eWaybillRepository.save(ewaybill);
            createdEWaybills.add(eWaybillMapper.toResponseDto(savedEWaybill));
            log.info("E-Waybill (from template) created for customer {} by user {}", customerId, currentUser.getUsername());
        }

        return createdEWaybills;
    }


}