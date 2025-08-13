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
import com.firinyonetim.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;

import com.firinyonetim.backend.ewaybill.dto.request.BulkSendRequest;
import com.firinyonetim.backend.ewaybill.dto.response.BulkSendResponseDto;
import com.firinyonetim.backend.ewaybill.dto.response.BulkSendResultDto;

import com.firinyonetim.backend.ewaybill.dto.turkcell.TurkcellApiRequest.NoteLine; // YENİ IMPORT



import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;
import java.math.RoundingMode; // YENİ IMPORT

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
    private final CustomerProductAssignmentRepository customerProductAssignmentRepository; // YENİ REPO
    private final RouteAssignmentRepository routeAssignmentRepository; // YENİ REPO


    // --- EKSİK OLAN ALANLAR BURAYA EKLENDİ ---
    @Value("${ewaybill.sender.vkn}")
    private String senderVkn;
    @Value("${ewaybill.sender.name}")
    private String senderName;
    @Value("${ewaybill.sender.surname}")
    private String senderSurname;
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
        Long customerId = ewaybill.getCustomer().getId();
        Map<Long, CustomerProductAssignment> assignmentsMap = customerProductAssignmentRepository
                .findByCustomerId(customerId).stream()
                .collect(Collectors.toMap(cpa -> cpa.getProduct().getId(), cpa -> cpa));

        Set<EWaybillItem> items = new HashSet<>();
        for (EWaybillItemRequest itemDto : itemRequests) {
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + itemDto.getProductId()));

            CustomerProductAssignment assignment = assignmentsMap.get(product.getId());
            if (assignment == null) {
                throw new IllegalStateException("Ürün '" + product.getName() + "' bu müşteriye atanmamış. İrsaliye oluşturulamaz.");
            }

            EWaybillItem item = new EWaybillItem();
            item.setEWaybill(ewaybill);
            item.setProduct(product);
            item.setProductNameSnapshot(product.getName());
            item.setQuantity(itemDto.getQuantity());

            item.setPriceVatExclusive(assignment.getFinalPriceVatExclusive());
            item.setPriceVatIncluded(assignment.getFinalPriceVatIncluded());

            // YENİ SATIR: KDV oranını ata
            item.setVatRate(product.getVatRate());

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

            List<RouteAssignment> assignments = routeAssignmentRepository.findByCustomerId(customerId);
            if (!assignments.isEmpty()) {
                Route route = assignments.get(0).getRoute();
                ewaybill.setPlateNumber(route.getPlaka());
                if(route.getDriver() != null) {
                    User driver = route.getDriver();
                    ewaybill.setCarrierName(driver.getName() + " " + driver.getSurname());
                    ewaybill.setCarrierVknTckn(driver.getTckn());
                }
            }

            Map<Long, CustomerProductAssignment> assignmentsMap = customerProductAssignmentRepository
                    .findByCustomerId(customerId).stream()
                    .collect(Collectors.toMap(cpa -> cpa.getProduct().getId(), cpa -> cpa));

            template.getItems().forEach(templateItem -> {
                CustomerProductAssignment assignment = assignmentsMap.get(templateItem.getProduct().getId());
                if (assignment == null) {
                    throw new IllegalStateException("Şablondaki ürün '" + templateItem.getProductNameSnapshot() + "' bu müşteriye atanmamış.");
                }

                EWaybillItem newItem = new EWaybillItem();
                newItem.setEWaybill(ewaybill);
                newItem.setProduct(templateItem.getProduct());
                newItem.setProductNameSnapshot(templateItem.getProductNameSnapshot());
                newItem.setQuantity(templateItem.getQuantity());
                newItem.setUnitCode(templateItem.getUnitCode());
                newItem.setPriceVatExclusive(assignment.getFinalPriceVatExclusive());
                newItem.setPriceVatIncluded(assignment.getFinalPriceVatIncluded());
                // YENİ SATIR: Şablondan oluştururken de KDV oranını ata
                newItem.setVatRate(templateItem.getProduct().getVatRate());
                ewaybill.getItems().add(newItem);
            });

            EWaybill savedEWaybill = eWaybillRepository.save(ewaybill);
            createdEWaybills.add(eWaybillMapper.toResponseDto(savedEWaybill));
            log.info("E-Waybill (from template) created for customer {} by user {}", customerId, currentUser.getUsername());
        }
        return createdEWaybills;
    }

    // GÜNCELLENDİ: Eski `sendEWaybill` metodu artık `sendSingleEWaybill`'i çağırıyor.
    @Transactional
    public void sendEWaybill(UUID ewaybillId) {
        BulkSendResultDto result = sendSingleEWaybill(ewaybillId);
        if (!result.isSuccess()) {
            // Hata varsa, exception fırlatarak eski davranışla uyumlu kalmasını sağla
            throw new RuntimeException(result.getMessage());
        }
    }

    private TurkcellApiRequest buildTurkcellRequest(EWaybill ewaybill) {
        TurkcellApiRequest request = eWaybillMapper.toTurkcellApiRequest(ewaybill);
        request.setLocalReferenceId(String.valueOf(ewaybill.getId()));
        Customer customer = ewaybill.getCustomer();
        Address address = customer.getAddress();
        TaxInfo taxInfo = customer.getTaxInfo();

        if (taxInfo == null) throw new IllegalStateException("E-irsaliye göndermek için müşteri vergi bilgisi gereklidir.");
        if (address == null) throw new IllegalStateException("E-irsaliye göndermek için müşteri adresi gereklidir.");

        EWaybillCustomerInfo customerInfo = eWaybillCustomerInfoRepository.findById(customer.getId())
                .orElseThrow(() -> new IllegalStateException("Müşteri '" + customer.getName() + "' için e-İrsaliye bilgisi yapılandırılmamış."));

        // ----- NOT ALANI GÜNCELLEMESİ BAŞLANGIÇ -----
        List<TurkcellApiRequest.NoteLine> notes = new ArrayList<>();

        // 1. Müşteri adını not olarak ekle
        TurkcellApiRequest.NoteLine customerNameNote = new TurkcellApiRequest.NoteLine();
        customerNameNote.setNote("Yetkili: " + customer.getName());
        notes.add(customerNameNote);

        // 2. Eğer kullanıcı tarafından girilmiş bir not varsa, onu da ekle
        if (StringUtils.hasText(ewaybill.getNotes())) {
            TurkcellApiRequest.NoteLine userNote = new TurkcellApiRequest.NoteLine();
            userNote.setNote(ewaybill.getNotes());
            notes.add(userNote);
        }
        System.out.println("Notlar: " + notes);
        // Not listesini ana isteğe ekle
        request.setNotes(notes);
        // ----- NOT ALANI GÜNCELLEMESİ BİTİŞ -----

        String targetAlias;
        if (customerInfo.getRecipientType() == EWaybillRecipientType.REGISTERED_USER) {
            targetAlias = customerInfo.getDefaultAlias();
        } else {
            targetAlias = "urn:mail:irsaliyepk@gib.gov.tr";
        }

        String taxNumber = taxInfo.getTaxNumber().replaceAll("\\s+", "");
        boolean isPerson = taxNumber.length() == 11;

        String buyerFirstName;
        String buyerLastName = null;

        String fullNameFromTaxInfo = taxInfo.getTradeName();
        if (!StringUtils.hasText(fullNameFromTaxInfo)) {
            throw new IllegalStateException("Müşteri (ID: " + customer.getId() + ") için TaxInfo'da ticari unvan (tradeName) boş olamaz.");
        }
        fullNameFromTaxInfo = fullNameFromTaxInfo.trim();

        if (isPerson) {
            int lastSpaceIndex = fullNameFromTaxInfo.lastIndexOf(' ');
            if (lastSpaceIndex > 0 && lastSpaceIndex < fullNameFromTaxInfo.length() - 1) {
                buyerFirstName = fullNameFromTaxInfo.substring(0, lastSpaceIndex);
                buyerLastName = fullNameFromTaxInfo.substring(lastSpaceIndex + 1);
            } else {
                buyerFirstName = fullNameFromTaxInfo;
            }
        } else {
            buyerFirstName = fullNameFromTaxInfo;
        }

        TurkcellApiRequest.AddressBook addressBook = new TurkcellApiRequest.AddressBook();
        addressBook.setIdentificationNumber(taxNumber);
        addressBook.setName(buyerFirstName);
        if (isPerson) {
            addressBook.setReceiverPersonSurName(buyerLastName);
        }
        addressBook.setAlias(targetAlias);
        addressBook.setReceiverStreet(address.getDetails());
        addressBook.setReceiverDistrict(address.getDistrict());
        addressBook.setReceiverCity(address.getProvince());
        addressBook.setReceiverCountry("Türkiye");
        addressBook.setReceiverPhoneNumber(customer.getPhone());
        addressBook.setReceiverEmail(customer.getEmail());
        addressBook.setReceiverTaxOffice(taxInfo.getTaxOffice());
        addressBook.setReceiverZipCode(address.getZipcode());
        request.setAddressBook(addressBook);

        TurkcellApiRequest.DespatchBuyerCustomerInfo buyerInfo = new TurkcellApiRequest.DespatchBuyerCustomerInfo();
        buyerInfo.setIdentificationNumber(taxNumber);
        buyerInfo.setName(buyerFirstName);
        if (isPerson) {
            buyerInfo.setPersonSurName(buyerLastName);
        }
        buyerInfo.setCity(address.getProvince());
        buyerInfo.setDistrict(address.getDistrict());
        buyerInfo.setCountryName("Türkiye");
        request.setDespatchBuyerCustomerInfo(buyerInfo);

        TurkcellApiRequest.DeliveryAddressInfo deliveryAddress = new TurkcellApiRequest.DeliveryAddressInfo();
        deliveryAddress.setStreet(address.getDetails());
        deliveryAddress.setCity(address.getProvince());
        deliveryAddress.setDistrict(address.getDistrict());
        deliveryAddress.setCountryName("Türkiye");
        deliveryAddress.setZipCode(address.getZipcode());
        request.setDeliveryAddressInfo(deliveryAddress);

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
        sellerInfo.setPersonSurName(senderSurname);
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

    // GÜNCELLENDİ: Bu metot artık `private` ve bir sonuç nesnesi dönüyor.
    private BulkSendResultDto sendSingleEWaybill(UUID ewaybillId) {
        EWaybill ewaybill = eWaybillRepository.findById(ewaybillId)
                .orElse(null);

        if (ewaybill == null) {
            return new BulkSendResultDto(ewaybillId, null, false, "İrsaliye bulunamadı.");
        }

        try {
            if (ewaybill.getStatus() != EWaybillStatus.DRAFT && ewaybill.getStatus() != EWaybillStatus.API_ERROR) {
                throw new IllegalStateException("Sadece DRAFT veya API_ERROR durumundaki irsaliyeler gönderilebilir.");
            }
            validateEWaybillDates(ewaybill.getIssueDate(), ewaybill.getIssueTime(), ewaybill.getShipmentDate());

            TurkcellApiRequest request = buildTurkcellRequest(ewaybill);

            TurkcellApiResponse apiResponse = turkcellClient.createEWaybill(request);

            ewaybill.setTurkcellApiId(apiResponse.getId());
            ewaybill.setEwaybillNumber(apiResponse.getDespatchAdviceNumber() != null ? apiResponse.getDespatchAdviceNumber() : apiResponse.getDespatchNumber());
            ewaybill.setStatus(EWaybillStatus.SENDING);
            ewaybill.setTurkcellStatus(apiResponse.getStatus());

            ewaybill.setStatusMessage("Successfully queued for sending.");
            eWaybillRepository.save(ewaybill);
            log.info("E-waybill {} successfully sent to Turkcell API. Turkcell ID: {}", ewaybillId, apiResponse.getId());
            return new BulkSendResultDto(ewaybillId, ewaybill.getEwaybillNumber(), true, "Başarıyla gönderim kuyruğuna alındı.");

        } catch (Exception e) {
            log.error("Error sending e-waybill {} to Turkcell API: {}", ewaybillId, e.getMessage());
            if (ewaybill.getStatus() != EWaybillStatus.SENDING) { // Eğer durum değişmediyse API_ERROR olarak işaretle
                ewaybill.setStatus(EWaybillStatus.API_ERROR);
                ewaybill.setStatusMessage("Failed to send to Turkcell API: " + e.getMessage());
                eWaybillRepository.save(ewaybill);
            }
            return new BulkSendResultDto(ewaybillId, ewaybill.getEwaybillNumber(), false, e.getMessage());
        }
    }


    // YENİ: Toplu gönderme metodu
    @Transactional
    public BulkSendResponseDto sendBulkEWaybills(BulkSendRequest request) {
        BulkSendResponseDto response = new BulkSendResponseDto();
        response.setTotalRequested(request.getEwaybillIds().size());

        for (UUID ewaybillId : request.getEwaybillIds()) {
            BulkSendResultDto result = sendSingleEWaybill(ewaybillId);
            response.getResults().add(result);
            if (result.isSuccess()) {
                response.setTotalSuccess(response.getTotalSuccess() + 1);
            } else {
                response.setTotalFailed(response.getTotalFailed() + 1);
            }
        }
        return response;
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

    // BU METODU BUL VE İÇERİĞİNİ AŞAĞIDAKİ İLE DEĞİŞTİR
    private void validateEWaybillDates(LocalDate issueDate, LocalTime issueTime, LocalDateTime shipmentDate) {
        LocalDateTime issueDateTime = issueDate.atTime(issueTime);

        // Kural 1: İrsaliye tarihi geçmiş bir gün olamaz.
        // Bu kontrol DTO seviyesinde @FutureOrPresent ile zaten yapılıyor, bu ek bir güvencedir.
        if (issueDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("İrsaliye tarihi geçmiş bir gün olamaz.");
        }

        // Kural 3: Sevk tarihi ve saati, irsaliye tarih ve saatinden önce olamaz. Eşit olabilir.
        if (shipmentDate.isBefore(issueDateTime)) {
            throw new IllegalArgumentException("Sevk tarihi ve saati, irsaliye tarih ve saatinden önce olamaz.");
        }
    }

}