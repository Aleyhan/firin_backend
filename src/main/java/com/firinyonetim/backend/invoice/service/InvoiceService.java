package com.firinyonetim.backend.invoice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.firinyonetim.backend.dto.PagedResponseDto;
import com.firinyonetim.backend.entity.*;
import com.firinyonetim.backend.exception.ResourceNotFoundException;
import com.firinyonetim.backend.invoice.dto.*;
import com.firinyonetim.backend.invoice.dto.turkcell.TurkcellInvoiceRequest;
import com.firinyonetim.backend.invoice.dto.turkcell.TurkcellInvoiceResponse;
import com.firinyonetim.backend.invoice.dto.turkcell.TurkcellInvoiceStatusResponse;
import com.firinyonetim.backend.invoice.entity.Invoice;
import com.firinyonetim.backend.invoice.entity.InvoiceItem;
import com.firinyonetim.backend.invoice.entity.InvoiceSettings;
import com.firinyonetim.backend.invoice.entity.InvoiceStatus;
import com.firinyonetim.backend.invoice.mapper.InvoiceMapper;
import com.firinyonetim.backend.invoice.mapper.InvoiceTurkcellMapper;
import com.firinyonetim.backend.invoice.repository.InvoiceRepository;
import com.firinyonetim.backend.repository.CustomerProductAssignmentRepository;
import com.firinyonetim.backend.repository.CustomerRepository;
import com.firinyonetim.backend.repository.ProductRepository;
import com.firinyonetim.backend.ewaybill.entity.EWaybill;
import com.firinyonetim.backend.ewaybill.entity.EWaybillItem;
import com.firinyonetim.backend.ewaybill.repository.EWaybillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpClientErrorException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final InvoiceMapper invoiceMapper;
    private final InvoiceSettingsService invoiceSettingsService;
    private final InvoiceTurkcellMapper invoiceTurkcellMapper;
    private final TurkcellInvoiceClient turkcellInvoiceClient;
    private final EWaybillRepository eWaybillRepository;
    private final ObjectMapper objectMapper;
    private final CustomerProductAssignmentRepository customerProductAssignmentRepository; // YENİ

    // ... (getAllInvoices, getInvoiceById, createDraftInvoice, updateDraftInvoice, deleteDraftInvoice, sendInvoice, checkAndUpdateStatuses, getInvoicePdf, getInvoiceHtml metotları aynı kalacak) ...
    @Transactional(readOnly = true)
    public PagedResponseDto<InvoiceResponse> getAllInvoices(String status, Pageable pageable) {
        Specification<Invoice> spec = (root, query, cb) -> {
            if (status != null && !status.isEmpty()) {
                List<InvoiceStatus> statusList = Arrays.stream(status.split(","))
                        .map(InvoiceStatus::valueOf)
                        .collect(Collectors.toList());
                return root.get("status").in(statusList);
            }
            return null;
        };
        Page<Invoice> invoicePage = invoiceRepository.findAll(spec, pageable);
        return new PagedResponseDto<>(invoicePage.map(invoiceMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceById(UUID id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));
        return invoiceMapper.toResponse(invoice);
    }

    @Transactional
    public InvoiceResponse createDraftInvoice(InvoiceCreateRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + request.getCustomerId()));

        Invoice invoice = new Invoice();
        invoice.setCustomer(customer);
        invoice.setCreatedBy(currentUser);
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setProfileType(request.getProfileType());
        invoice.setType(request.getType());
        invoice.setIssueDate(request.getIssueDate());
        invoice.setCurrencyCode(request.getCurrencyCode());
        invoice.setNotes(request.getNotes());

        Set<InvoiceItem> items = processInvoiceItems(request.getItems(), invoice);
        invoice.setItems(items);
        calculateTotals(invoice);
        handleRelatedEWaybills(invoice, request.getRelatedEWaybillIds());


        Invoice savedInvoice = invoiceRepository.save(invoice);
        log.info("Draft invoice created with id: {}", savedInvoice.getId());
        return invoiceMapper.toResponse(savedInvoice);
    }

    @Transactional
    public InvoiceResponse updateDraftInvoice(UUID id, InvoiceCreateRequest request) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));

        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT invoices can be updated.");
        }

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + request.getCustomerId()));

        invoice.setCustomer(customer);
        invoice.setProfileType(request.getProfileType());
        invoice.setType(request.getType());
        invoice.setIssueDate(request.getIssueDate());
        invoice.setCurrencyCode(request.getCurrencyCode());
        invoice.setNotes(request.getNotes());

        invoice.getItems().clear();
        Set<InvoiceItem> newItems = processInvoiceItems(request.getItems(), invoice);
        invoice.getItems().addAll(newItems);

        calculateTotals(invoice);
        handleRelatedEWaybills(invoice, request.getRelatedEWaybillIds());

        Invoice updatedInvoice = invoiceRepository.save(invoice);
        log.info("Draft invoice updated with id: {}", updatedInvoice.getId());
        return invoiceMapper.toResponse(updatedInvoice);
    }

    private void handleRelatedEWaybills(Invoice invoice, List<UUID> ewaybillIds) {
        if (CollectionUtils.isEmpty(ewaybillIds)) {
            invoice.setRelatedDespatchesJson(null);
            return;
        }

        List<EWaybill> ewaybills = eWaybillRepository.findAllById(ewaybillIds);
        List<UUID> foundIds = ewaybills.stream().map(EWaybill::getId).collect(Collectors.toList());
        if (!new HashSet<>(ewaybillIds).equals(new HashSet<>(foundIds))) {
            throw new ResourceNotFoundException("Gönderilen irsaliye ID'lerinden bazıları veritabanında bulunamadı.");
        }

        // DEĞİŞİKLİK: Yeni DTO yapısına göre nesne oluşturuluyor
        List<EWaybillForInvoiceDto> relatedDespatches = ewaybills.stream()
                .map(ew -> new EWaybillForInvoiceDto(ew.getId(), ew.getEwaybillNumber(), ew.getIssueDate().atTime(ew.getIssueTime())))
                .collect(Collectors.toList());
        try {
            invoice.setRelatedDespatchesJson(objectMapper.writeValueAsString(relatedDespatches));
        } catch (JsonProcessingException e) {
            log.error("Could not serialize related despatches for invoice {}", invoice.getId(), e);
            invoice.setRelatedDespatchesJson(null);
        }
    }


    @Transactional
    public void deleteDraftInvoice(UUID id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));

        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT invoices can be deleted.");
        }

        invoiceRepository.delete(invoice);
        log.info("Draft invoice deleted with id: {}", id);
    }

    @Transactional
    public InvoiceResponse sendInvoice(UUID id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));

        if (invoice.getStatus() != InvoiceStatus.DRAFT && invoice.getStatus() != InvoiceStatus.API_ERROR) {
            throw new IllegalStateException("Only invoices in DRAFT or API_ERROR status can be sent.");
        }

        InvoiceSettings settings = invoiceSettingsService.getSettings();
        TurkcellInvoiceRequest request = invoiceTurkcellMapper.toTurkcellRequest(invoice, settings);

        invoice.setStatus(InvoiceStatus.SENDING);
        invoiceRepository.save(invoice);
try {
            System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request));
        } catch (JsonProcessingException e) {
            System.out.println(request);
        }
        try {
            TurkcellInvoiceResponse response = turkcellInvoiceClient.createInvoice(request);
            invoice.setTurkcellApiId(response.getId());
            invoice.setInvoiceNumber(response.getInvoiceNumber());
            invoice.setStatusMessage("Successfully queued for sending.");
            log.info("Invoice {} successfully sent to Turkcell API. Turkcell ID: {}", id, response.getId());
        } catch (HttpClientErrorException e) {
            log.error("Error sending invoice {} to Turkcell API: {}", id, e.getResponseBodyAsString());
            invoice.setStatus(InvoiceStatus.API_ERROR);
            invoice.setStatusMessage("API Error: " + e.getResponseBodyAsString());
            throw new RuntimeException("Failed to send invoice to Turkcell API: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Unexpected error sending invoice {}: {}", id, e.getMessage());
            invoice.setStatus(InvoiceStatus.API_ERROR);
            invoice.setStatusMessage("Unexpected Error: " + e.getMessage());
            throw new RuntimeException("An unexpected error occurred while sending the invoice.");
        } finally {
            invoiceRepository.save(invoice);
        }

        return invoiceMapper.toResponse(invoice);
    }

    @Transactional
    public void checkAndUpdateStatuses() {
        List<Invoice> invoicesToCheck = invoiceRepository.findByStatus(InvoiceStatus.SENDING);
        if (invoicesToCheck.isEmpty()) {
            return;
        }
        log.info("Found {} invoices with SENDING status to check.", invoicesToCheck.size());

        for (Invoice invoice : invoicesToCheck) {
            if (invoice.getTurkcellApiId() == null) {
                log.warn("Invoice with internal id {} has SENDING status but no Turkcell API ID. Skipping.", invoice.getId());
                continue;
            }

            try {
                TurkcellInvoiceStatusResponse response = turkcellInvoiceClient.getInvoiceStatus(invoice.getTurkcellApiId());
                updateStatusFromTurkcellResponse(invoice, response);
                invoiceRepository.save(invoice);
            } catch (Exception e) {
                log.error("Failed to check status for invoice with Turkcell ID {}: {}", invoice.getTurkcellApiId(), e.getMessage());
            }
        }
    }

    @Transactional(readOnly = true)
    public byte[] getInvoicePdf(UUID id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));
        if (invoice.getTurkcellApiId() == null) {
            throw new IllegalStateException("This invoice has not been sent to the provider yet.");
        }
        return turkcellInvoiceClient.getInvoiceAsPdf(invoice.getTurkcellApiId());
    }

    @Transactional(readOnly = true)
    public String getInvoiceHtml(UUID id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));
        if (invoice.getTurkcellApiId() == null) {
            throw new IllegalStateException("This invoice has not been sent to the provider yet.");
        }
        return turkcellInvoiceClient.getInvoiceAsHtml(invoice.getTurkcellApiId());
    }

    @Transactional(readOnly = true)
    public List<EWaybillForInvoiceDto> getUninvoicedEWaybills(Long customerId) {
        // DEĞİŞİKLİK: Yeni DTO yapısına göre nesne oluşturuluyor
        return eWaybillRepository.findUninvoicedEWaybillsByCustomerId(customerId).stream()
                .map(ew -> new EWaybillForInvoiceDto(ew.getId(), ew.getEwaybillNumber(), ew.getIssueDate().atTime(ew.getIssueTime())))
                .collect(Collectors.toList());
    }


    @Transactional(readOnly = true)
    public InvoiceCalculationResponse calculateItemsFromEwaybills(Long customerId, List<UUID> ewaybillIds) {
        InvoiceCalculationResponse response = new InvoiceCalculationResponse();
        Set<String> warnings = new HashSet<>(); // Uyarı mekanizmasını koruyoruz.

        List<EWaybill> ewaybills = eWaybillRepository.findAllById(ewaybillIds);
        if (ewaybills.isEmpty()) {
            throw new IllegalArgumentException("Hesaplama için en az bir geçerli irsaliye seçilmelidir.");
        }

        if (!ewaybills.stream().allMatch(e -> e.getCustomer().getId().equals(customerId))) {
            throw new IllegalArgumentException("Seçilen irsaliyeler, belirtilen müşteri ile eşleşmiyor.");
        }

        // BU METODUN İÇERİĞİNİ GÜNCELLE
        Map<Long, BigDecimal> productQuantities = ewaybills.stream()
                .flatMap(e -> e.getItems().stream())
                .collect(Collectors.groupingBy(
                        item -> item.getProduct().getId(),
                        Collectors.reducing(BigDecimal.ZERO, EWaybillItem::getQuantity, BigDecimal::add)
                ));


        // Her ürün için ilk karşılaşılan irsaliye kalemini (fiyat ve kdv oranını almak için) bul
        Map<Long, EWaybillItem> firstItemMap = ewaybills.stream()
                .flatMap(e -> e.getItems().stream())
                .collect(Collectors.toMap(
                        item -> item.getProduct().getId(),
                        Function.identity(),
                        (existing, replacement) -> existing // Eğer aynı ürün birden fazla irsaliyede varsa ilkini koru
                ));

        List<CalculatedInvoiceItemDto> calculatedItems = new ArrayList<>();
        productQuantities.forEach((productId, totalQuantity) -> {
            EWaybillItem representativeItem = firstItemMap.get(productId);
            if (representativeItem == null) {
                // Bu durumun oluşmaması gerekir ama bir güvenlik önlemi
                throw new IllegalStateException("İrsaliyelerde bulunan bir ürün için kalem bilgisi bulunamadı: " + productId);
            }

            CalculatedInvoiceItemDto dto = new CalculatedInvoiceItemDto();
            dto.setProductId(productId);
            dto.setProductName(representativeItem.getProductNameSnapshot());
            dto.setQuantity(totalQuantity.intValue()); // Miktarı toplanmış miktar olarak ayarla

            // Fiyatı ve KDV oranını doğrudan irsaliye kaleminden al
            dto.setUnitPrice(representativeItem.getPriceVatExclusive());
            // Not: InvoiceItemRequest'te vatRate alanı yok, bu bilgi Product'tan alınacak.
            // Bu yüzden dto'ya eklemeye gerek yok, sadece unitPrice'ı doğru göndermemiz yeterli.

            calculatedItems.add(dto);
        });

        response.setItems(calculatedItems);
        response.setWarnings(new ArrayList<>(warnings)); // Uyarı listesini (şimdilik boş) yanıta ekle
        return response;
    }


    private void updateStatusFromTurkcellResponse(Invoice invoice, TurkcellInvoiceStatusResponse response) {
        invoice.setTurkcellStatus(response.getStatus());
        invoice.setStatusMessage(response.getMessage());

        switch (response.getStatus()) {
            case 60:
                invoice.setStatus(InvoiceStatus.APPROVED);
                break;
            case 40:
                invoice.setStatus(InvoiceStatus.REJECTED_BY_GIB);
                break;
            default:
                log.info("Invoice {} has Turkcell status: {}. Keeping internal status as SENDING.", invoice.getId(), response.getStatus());
                break;
        }
        log.info("Updated status for invoice {}. New Turkcell status: {}, New internal status: {}",
                invoice.getId(), invoice.getTurkcellStatus(), invoice.getStatus());
    }

    private Set<InvoiceItem> processInvoiceItems(Iterable<InvoiceItemRequest> itemRequests, Invoice invoice) {
        // BU METODUN İÇERİĞİNİ GÜNCELLE
        Set<InvoiceItem> items = new HashSet<>();
        for (InvoiceItemRequest itemRequest : itemRequests) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + itemRequest.getProductId()));

            InvoiceItem item = new InvoiceItem();
            item.setInvoice(invoice);
            item.setProduct(product);
            item.setQuantity(itemRequest.getQuantity());
            item.setUnitPrice(itemRequest.getUnitPrice());
            item.setDescription(itemRequest.getDescription());
            item.setDiscountAmount(itemRequest.getDiscountAmount() != null ? itemRequest.getDiscountAmount() : BigDecimal.ZERO);

            // YENİ: KDV oranını product yerine request'ten alıyoruz
            Integer vatRateValue = itemRequest.getVatRate();
            item.setVatRate(vatRateValue);

            BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())).subtract(item.getDiscountAmount());
            item.setTotalPrice(lineTotal);

            BigDecimal vatRateDecimal = BigDecimal.valueOf(vatRateValue).divide(BigDecimal.valueOf(100));
            BigDecimal vatAmount = lineTotal.multiply(vatRateDecimal).setScale(2, RoundingMode.HALF_UP);
            item.setVatAmount(vatAmount);

            items.add(item);
        }
        return items;
    }

    private void calculateTotals(Invoice invoice) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalVatAmount = BigDecimal.ZERO;

        for (InvoiceItem item : invoice.getItems()) {
            totalAmount = totalAmount.add(item.getTotalPrice());
            totalVatAmount = totalVatAmount.add(item.getVatAmount());
        }

        invoice.setTotalAmount(totalAmount);
        invoice.setTotalVatAmount(totalVatAmount);
        invoice.setPayableAmount(totalAmount.add(totalVatAmount));
    }
}