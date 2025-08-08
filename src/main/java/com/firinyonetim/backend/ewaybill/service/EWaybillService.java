// src/main/java/com/firinyonetim/backend/ewaybill/service/EWaybillService.java
package com.firinyonetim.backend.ewaybill.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.firinyonetim.backend.entity.*;
import com.firinyonetim.backend.ewaybill.dto.request.BulkEWaybillFromTemplateRequest;
import com.firinyonetim.backend.ewaybill.dto.request.EWaybillCreateRequest;
import com.firinyonetim.backend.ewaybill.dto.request.EWaybillItemRequest;
import com.firinyonetim.backend.ewaybill.dto.response.EWaybillResponse;
import com.firinyonetim.backend.ewaybill.dto.turkcell.TurkcellApiRequest;
import com.firinyonetim.backend.ewaybill.dto.turkcell.TurkcellApiResponse;
import com.firinyonetim.backend.ewaybill.entity.*;
import com.firinyonetim.backend.ewaybill.mapper.EWaybillMapper;
import com.firinyonetim.backend.ewaybill.repository.EWaybillCustomerInfoRepository;
import com.firinyonetim.backend.ewaybill.repository.EWaybillRepository;
import com.firinyonetim.backend.ewaybill.repository.EWaybillTemplateRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.*;
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

    // --- EKSİK OLAN ALANLAR BURAYA EKLENDİ ---
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
    // --- EKSİK ALANLARIN SONU ---

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

        EWaybill ewaybill = new EWaybill();
        ewaybill.setCreatedBy(currentUser);
        ewaybill.setCustomer(customer);
        eWaybillMapper.updateFromRequest(request, ewaybill);

        if (request.getRouteId() != null) {
            Route route = routeRepository.findById(request.getRouteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + request.getRouteId()));
            ewaybill.setPlateNumber(route.getPlaka());
        }
        try {
            ewaybill.setDeliveryAddressJson(objectMapper.writeValueAsString(customer.getAddress()));
        } catch (JsonProcessingException e) {
            log.error("Could not serialize delivery address for customer {}", customer.getId(), e);
            throw new RuntimeException("Delivery address could not be processed.");
        }

        Set<EWaybillItem> items = processItems(request.getItems(), ewaybill);
        ewaybill.getItems().addAll(items);

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

        if (request.getRouteId() != null) {
            Route route = routeRepository.findById(request.getRouteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + request.getRouteId()));
            ewaybill.setPlateNumber(route.getPlaka());
        } else {
            ewaybill.setPlateNumber(null);
        }

        ewaybill.getItems().clear();
        Set<EWaybillItem> updatedItems = processItems(request.getItems(), ewaybill);
        ewaybill.getItems().addAll(updatedItems);

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

            String unitCode = "C62";
            if (product.getUnit() != null && StringUtils.hasText(product.getUnit().getCode())) {
                unitCode = product.getUnit().getCode();
            }
            item.setUnitCode(unitCode);
            items.add(item);
        }
        return items;
    }

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
                    .orElseThrow(() -> new IllegalStateException("Şablon bulunamadı, Müşteri: " + customer.getName()));

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

            template.getItems().forEach(templateItem -> {
                EWaybillItem newItem = new EWaybillItem();
                newItem.setEWaybill(ewaybill);
                newItem.setProduct(templateItem.getProduct());
                newItem.setProductNameSnapshot(templateItem.getProductNameSnapshot());
                newItem.setQuantity(templateItem.getQuantity());
                newItem.setUnitCode(templateItem.getUnitCode());
                ewaybill.getItems().add(newItem);
            });

            EWaybill savedEWaybill = eWaybillRepository.save(ewaybill);
            createdEWaybills.add(eWaybillMapper.toResponseDto(savedEWaybill));
            log.info("E-Waybill (from template) created for customer {} by user {}", customerId, currentUser.getUsername());
        }

        return createdEWaybills;
    }

    @Transactional
    public void sendEWaybill(UUID ewaybillId) {
        log.info("Sending e-waybill with internal id: {}", ewaybillId);
        EWaybill ewaybill = eWaybillRepository.findById(ewaybillId)
                .orElseThrow(() -> new ResourceNotFoundException("EWaybill not found with id: " + ewaybillId));

        if (ewaybill.getStatus() != EWaybillStatus.DRAFT && ewaybill.getStatus() != EWaybillStatus.API_ERROR) {
            throw new IllegalStateException("Only e-waybills in DRAFT or API_ERROR status can be sent.");
        }

        // --- YENİ: VALIDASYON ÇAĞRISI BURAYA EKLENDİ ---
        validateEWaybillDates(ewaybill.getIssueDate(), ewaybill.getIssueTime(), ewaybill.getShipmentDate());
        // --- VALIDASYON ÇAĞRISI SONU ---


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
        addressBook.setAlias(targetAlias);
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
            line.setManufacturersItemIdentification(item.getProduct().getId().toString());
            despatchLines.add(line);
        }
        request.setDespatchLines(despatchLines);

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

    private void validateIssueDate(Customer customer, java.time.LocalDate issueDate) {
        Set<DayOfWeek> irsaliyeGunleri = customer.getIrsaliyeGunleri();
        if (irsaliyeGunleri != null && !irsaliyeGunleri.isEmpty()) {
            java.time.DayOfWeek dayOfWeek = issueDate.getDayOfWeek();

            DayOfWeek bizimDayOfWeek;
            try {
                bizimDayOfWeek = DayOfWeek.valueOf(dayOfWeek.name());
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("Invalid day of week: " + dayOfWeek.name());
            }

            if (!irsaliyeGunleri.contains(bizimDayOfWeek)) {
                String dayName = dayOfWeek.getDisplayName(TextStyle.FULL, new Locale("tr", "TR"));
                throw new IllegalArgumentException(
                        "'" + customer.getName() + "' için irsaliye oluşturulamaz: Seçilen tarih (" + dayName + ") müşterinin irsaliye günlerinden biri değil."
                );
            }
        } else {
            throw new IllegalArgumentException(
                    "'" + customer.getName() + "' için irsaliye oluşturulamaz: Bu müşteri için herhangi bir irsaliye günü tanımlanmamış."
            );
        }
    }

    // --- VALIDASYON METODU BURADA KALIYOR ---
    private void validateEWaybillDates(LocalDate issueDate, LocalTime issueTime, LocalDateTime shipmentDate) {
        LocalDateTime issueDateTime = issueDate.atTime(issueTime);

        // Kural 1: Sevk tarihi, irsaliye tarihinden önce olamaz.
        if (shipmentDate.isBefore(issueDateTime)) {
            throw new IllegalArgumentException("Sevk tarihi, irsaliye tarihinden önce olamaz.");
        }

        // Kural 2: İrsaliye tarihi bugün ise, irsaliye saati geçmiş bir saat olamaz.
        if (issueDate.isEqual(LocalDate.now()) && issueTime.isBefore(LocalTime.now().minusMinutes(2))) {
            throw new IllegalArgumentException("İrsaliye saati taslakta geçmiş kalmış, bu gönderim saatiyle uyuşmuyor.");
        }
    }

}