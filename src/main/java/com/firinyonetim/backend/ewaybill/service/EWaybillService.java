package com.firinyonetim.backend.ewaybill.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.firinyonetim.backend.entity.Address;
import com.firinyonetim.backend.entity.Customer;
import com.firinyonetim.backend.entity.Product;
import com.firinyonetim.backend.entity.TaxInfo;
import com.firinyonetim.backend.entity.User;
import com.firinyonetim.backend.ewaybill.dto.request.EWaybillCreateRequest;
import com.firinyonetim.backend.ewaybill.dto.request.EWaybillItemRequest;
import com.firinyonetim.backend.ewaybill.dto.response.EWaybillResponse;
import com.firinyonetim.backend.ewaybill.dto.turkcell.TurkcellApiRequest;
import com.firinyonetim.backend.ewaybill.dto.turkcell.TurkcellApiResponse;
import com.firinyonetim.backend.ewaybill.entity.EWaybill;
import com.firinyonetim.backend.ewaybill.entity.EWaybillItem;
import com.firinyonetim.backend.ewaybill.entity.EWaybillStatus;
import com.firinyonetim.backend.ewaybill.mapper.EWaybillMapper;
import com.firinyonetim.backend.ewaybill.repository.EWaybillRepository;
import com.firinyonetim.backend.exception.ResourceNotFoundException;
import com.firinyonetim.backend.repository.CustomerRepository;
import com.firinyonetim.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
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
    private final TurkcellEWaybillClient turkcellClient;
    private final EWaybillMapper eWaybillMapper;
    private final ObjectMapper objectMapper;

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

        EWaybill ewaybill = eWaybillMapper.fromCreateRequest(request);
        ewaybill.setCreatedBy(currentUser);
        ewaybill.setCustomer(customer);

        try {
            ewaybill.setDeliveryAddressJson(objectMapper.writeValueAsString(customer.getAddress()));
        } catch (JsonProcessingException e) {
            log.error("Could not serialize delivery address for customer {}", customer.getId(), e);
            throw new RuntimeException("Delivery address could not be processed.");
        }

        Set<EWaybillItem> items = processItems(request.getItems(), ewaybill);
        ewaybill.setItems(items);

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

        eWaybillMapper.updateFromRequest(request, ewaybill);

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
            item.setUnitPrice(itemDto.getUnitPrice());
            item.setUnitCode("C62"); // TODO: Bu alan dinamik olmalı
            item.setLineAmount(itemDto.getQuantity().multiply(itemDto.getUnitPrice()));
            items.add(item);
        }
        return items;
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
            log.info("Turkcell API response: {}", objectMapper.writeValueAsString(response));
            ewaybill.setTurkcellApiId(response.getId());
            ewaybill.setEwaybillNumber(response.getDespatchAdviceNumber() != null ? response.getDespatchAdviceNumber() : response.getDespatchNumber());
            ewaybill.setStatus(EWaybillStatus.SENDING);
            ewaybill.setTurkcellStatus(response.getStatus());
            ewaybill.setStatusMessage("Successfully queued for sending.");

            eWaybillRepository.save(ewaybill);
            log.info("E-waybill {} successfully sent to Turkcell API. Turkcell ID: {}", ewaybillId, response.getId());

        } catch (HttpClientErrorException e) { // DÜZELTME: Genel Exception yerine spesifik hatayı yakala
            // HttpClientErrorException, 4xx ve 5xx hatalarını içerir ve response body'sine erişim sağlar.
            String responseBody = e.getResponseBodyAsString();
            log.error("Error sending e-waybill {} to Turkcell API. Status: {}, Body: {}", ewaybillId, e.getStatusCode(), responseBody);

            ewaybill.setStatus(EWaybillStatus.API_ERROR);
            ewaybill.setStatusMessage(responseBody); // Hatanın JSON gövdesini direkt kaydet
            eWaybillRepository.save(ewaybill);

            // Frontend'e de bu detaylı JSON mesajını fırlat
            throw new RuntimeException(responseBody, e);

        } catch (Exception e) { // Diğer tüm hatalar için (örn: ağ bağlantısı yok)
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

        // AddressBook nesnesini zenginleştir
        TurkcellApiRequest.AddressBook addressBook = new TurkcellApiRequest.AddressBook();
        addressBook.setIdentificationNumber(taxInfo.getTaxNumber());
        addressBook.setName(customer.getName());
        addressBook.setAlias("urn:mail:defaulttest3pk@medyasoft.com.tr"); // TODO: Dinamik yap
        addressBook.setReceiverCity(address.getProvince()); // YENİ
        addressBook.setReceiverDistrict(address.getDistrict()); // YENİ
        addressBook.setReceiverCountry("Türkiye"); // YENİ
        request.setAddressBook(addressBook);

        // DeliveryAddressInfo (Bu zaten doğruydu)
        TurkcellApiRequest.DeliveryAddressInfo deliveryAddress = new TurkcellApiRequest.DeliveryAddressInfo();
        deliveryAddress.setCity(address.getProvince());
        deliveryAddress.setDistrict(address.getDistrict());
        deliveryAddress.setCountryName("Türkiye");
        deliveryAddress.setZipCode("34000"); // TODO: Dinamik yap
        request.setDeliveryAddressInfo(deliveryAddress);

        // DespatchBuyerCustomerInfo (Bu zaten doğruydu)
        TurkcellApiRequest.DespatchBuyerCustomerInfo buyerInfo = new TurkcellApiRequest.DespatchBuyerCustomerInfo();
        buyerInfo.setIdentificationNumber(taxInfo.getTaxNumber());
        buyerInfo.setName(customer.getName());
        buyerInfo.setCity(address.getProvince());
        buyerInfo.setDistrict(address.getDistrict());
        buyerInfo.setCountryName("Türkiye");
        request.setDespatchBuyerCustomerInfo(buyerInfo);

        // DespatchShipmentInfo (Bu zaten doğruydu)
        TurkcellApiRequest.DespatchShipmentInfo shipmentInfo = new TurkcellApiRequest.DespatchShipmentInfo();
        shipmentInfo.setShipmentSenderTitle(ewaybill.getCarrierName());
        shipmentInfo.setShipmentSenderTcknVkn(ewaybill.getCarrierVknTckn());
        request.setDespatchShipmentInfo(shipmentInfo);

        // SellerSupplierInfo'yu güncelle
        TurkcellApiRequest.SellerSupplierInfo sellerInfo = new TurkcellApiRequest.SellerSupplierInfo();
        sellerInfo.setIdentificationNumber(senderVkn);
        sellerInfo.setName(senderName);
        sellerInfo.setPersonSurName("-"); // YENİ: Zorunlu alan için placeholder
        sellerInfo.setCity(senderCity);
        sellerInfo.setDistrict(senderDistrict);
        sellerInfo.setCountryName(senderCountry);
        request.setSellerSupplierInfo(sellerInfo);

        return request;
    }

    @Transactional
    public void checkAndUpdateStatuses() {
        // DÜZELTME: Yeni repository metodunu kullanıyoruz.
        List<EWaybill> ewaybillsToCheck = eWaybillRepository.findEWaybillsToQueryStatus();

        if (ewaybillsToCheck.isEmpty()) {
            return;
        }
        log.info("Found {} e-waybills with SENDING or AWAITING_APPROVAL status to check.", ewaybillsToCheck.size());

        for (EWaybill ewaybill : ewaybillsToCheck) {
            // turkcellApiId'si olmayan bir kayıt varsa (beklenmedik bir durum), atla.
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

    // YENİ METOT
    @Transactional(readOnly = true)
    public byte[] getEWaybillPdf(UUID id) {
        EWaybill ewaybill = eWaybillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("E-Waybill not found with id: " + id));
        if (ewaybill.getTurkcellApiId() == null) {
            throw new IllegalStateException("This e-waybill has not been sent to the provider yet.");
        }
        return turkcellClient.getEWaybillAsPdf(ewaybill.getTurkcellApiId());
    }

    // YENİ METOT
    @Transactional(readOnly = true)
    public String getEWaybillHtml(UUID id) {
        EWaybill ewaybill = eWaybillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("E-Waybill not found with id: " + id));
        if (ewaybill.getTurkcellApiId() == null) {
            throw new IllegalStateException("This e-waybill has not been sent to the provider yet.");
        }
        return turkcellClient.getEWaybillAsHtml(ewaybill.getTurkcellApiId());
    }
}