package com.firinyonetim.backend.invoice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.firinyonetim.backend.dto.PagedResponseDto;
import com.firinyonetim.backend.entity.*;
import com.firinyonetim.backend.ewaybill.dto.response.EWaybillItemResponse;
import com.firinyonetim.backend.ewaybill.entity.EWaybillStatus;
import com.firinyonetim.backend.ewaybill.mapper.EWaybillMapper;
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
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final CustomerProductAssignmentRepository customerProductAssignmentRepository;
    private final EWaybillMapper eWaybillMapper; // YENİ MAPPER



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
        validateEwaybillsNotInvoiced(request.getRelatedEWaybillIds(), null);
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

        validateEwaybillsNotInvoiced(request.getRelatedEWaybillIds(), id);


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
            // Eğer eskisinde irsaliye vardı ama yenisinde yoksa, eski irsaliyelerden fatura bilgisini temizle
            if (StringUtils.hasText(invoice.getRelatedDespatchesJson())) {
                try {
                    List<Map<String, String>> oldDespatches = objectMapper.readValue(invoice.getRelatedDespatchesJson(), new TypeReference<>() {});
                    List<UUID> oldEwaybillIds = oldDespatches.stream().map(d -> UUID.fromString(d.get("id"))).collect(Collectors.toList());
                    List<EWaybill> ewaybillsToClear = eWaybillRepository.findAllById(oldEwaybillIds);
                    ewaybillsToClear.forEach(ew -> {
                        ew.setInvoiceId(null);
                        ew.setInvoiceNumber(null);
                    });
                    eWaybillRepository.saveAll(ewaybillsToClear);
                } catch (JsonProcessingException e) {
                    log.error("Could not parse old related despatches for cleanup on invoice {}", invoice.getId(), e);
                }
            }
            invoice.setRelatedDespatchesJson(null);
            return;
        }

        List<EWaybill> ewaybills = eWaybillRepository.findAllById(ewaybillIds);
        List<UUID> foundIds = ewaybills.stream().map(EWaybill::getId).collect(Collectors.toList());
        if (!new HashSet<>(ewaybillIds).equals(new HashSet<>(foundIds))) {
            throw new ResourceNotFoundException("Gönderilen irsaliye ID'lerinden bazıları veritabanında bulunamadı.");
        }

        // İrsaliyelerdeki invoiceId ve invoiceNumber alanlarını güncelle
        ewaybills.forEach(ew -> {
            ew.setInvoiceId(invoice.getId());
            ew.setInvoiceNumber(invoice.getInvoiceNumber()); // Henüz numara yoksa null olacak
        });
        eWaybillRepository.saveAll(ewaybills);

        List<EWaybillForInvoiceDto> relatedDespatches = ewaybills.stream()
                .map(ew -> {
                    EWaybillForInvoiceDto dto = new EWaybillForInvoiceDto();
                    dto.setId(ew.getId());
                    dto.setDespatchNumber(ew.getEwaybillNumber());
                    dto.setIssueDate(ew.getIssueDate().atTime(ew.getIssueTime()));
                    dto.setCustomerName(ew.getCustomer().getName());
                    return dto;
                })
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
        // Gönderimden önce save ederek 'SENDING' durumunu DB'ye yansıt
        invoiceRepository.save(invoice);

        try {
            TurkcellInvoiceResponse response = turkcellInvoiceClient.createInvoice(request);
            invoice.setTurkcellApiId(response.getId());
            invoice.setInvoiceNumber(response.getInvoiceNumber());
            invoice.setStatusMessage("Successfully queued for sending.");
            log.info("Invoice {} successfully sent to Turkcell API. Turkcell ID: {}", id, response.getId());

            // Fatura numarası artık belli olduğu için, ilişkili irsaliyeleri tekrar güncelle
            if (StringUtils.hasText(invoice.getRelatedDespatchesJson())) {
                try {
                    List<Map<String, String>> despatches = objectMapper.readValue(invoice.getRelatedDespatchesJson(), new TypeReference<>() {});
                    List<UUID> ewaybillIds = despatches.stream().map(d -> UUID.fromString(d.get("id"))).collect(Collectors.toList());
                    if (!ewaybillIds.isEmpty()) {
                        List<EWaybill> relatedEwaybills = eWaybillRepository.findAllById(ewaybillIds);
                        relatedEwaybills.forEach(ew -> {
                            ew.setInvoiceId(invoice.getId());
                            ew.setInvoiceNumber(invoice.getInvoiceNumber());
                        });
                        eWaybillRepository.saveAll(relatedEwaybills);
                    }
                } catch (JsonProcessingException e) {
                    log.error("Could not parse related despatches to update invoice number for invoice {}", invoice.getId(), e);
                }
            }

        } catch (HttpClientErrorException e) {
            log.error("Error sending invoice {} to Turkcell API: {}", id, e.getResponseBodyAsString());
            invoice.setStatus(InvoiceStatus.API_ERROR);
            invoice.setStatusMessage("API Error: " + e.getResponseBodyAsString());
            // Başarısız gönderim durumunda ilişkili irsaliyelerden fatura bilgisini temizle
            clearInvoiceInfoFromEwaybills(invoice);
            throw new RuntimeException("Failed to send invoice to Turkcell API: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Unexpected error sending invoice {}: {}", id, e.getMessage());
            invoice.setStatus(InvoiceStatus.API_ERROR);
            invoice.setStatusMessage("Unexpected Error: " + e.getMessage());
            clearInvoiceInfoFromEwaybills(invoice);
            throw new RuntimeException("An unexpected error occurred while sending the invoice.");
        } finally {
            invoiceRepository.save(invoice);
        }

        return invoiceMapper.toResponse(invoice);
    }

    // YENİ YARDIMCI METOT
    private void clearInvoiceInfoFromEwaybills(Invoice invoice) {
        if (StringUtils.hasText(invoice.getRelatedDespatchesJson())) {
            try {
                List<Map<String, String>> despatches = objectMapper.readValue(invoice.getRelatedDespatchesJson(), new TypeReference<>() {});
                List<UUID> ewaybillIds = despatches.stream().map(d -> UUID.fromString(d.get("id"))).collect(Collectors.toList());
                if (!ewaybillIds.isEmpty()) {
                    List<EWaybill> relatedEwaybills = eWaybillRepository.findAllById(ewaybillIds);
                    relatedEwaybills.forEach(ew -> {
                        ew.setInvoiceId(null);
                        ew.setInvoiceNumber(null);
                    });
                    eWaybillRepository.saveAll(relatedEwaybills);
                }
            } catch (JsonProcessingException e) {
                log.error("Could not parse related despatches for cleanup on failed send for invoice {}", invoice.getId(), e);
            }
        }
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
    public List<EWaybillForInvoiceDto> findUninvoicedEWaybills(Long mainCustomerId, List<Long> searchCustomerIds, LocalDate startDate, LocalDate endDate) {
        Customer mainCustomer = customerRepository.findById(mainCustomerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + mainCustomerId));

        TaxInfo taxInfo = mainCustomer.getTaxInfo();

        final List<Long> finalCustomerIdsToSearch;

        if (taxInfo == null || !StringUtils.hasText(taxInfo.getTaxNumber())) {
            finalCustomerIdsToSearch = List.of(mainCustomerId);
        } else {
            if (searchCustomerIds == null || searchCustomerIds.isEmpty()) {
                finalCustomerIdsToSearch = customerRepository.findByTaxInfoTaxNumber(taxInfo.getTaxNumber())
                        .stream()
                        .map(Customer::getId)
                        .collect(Collectors.toList());
            } else {
                finalCustomerIdsToSearch = searchCustomerIds;
            }
        }

        if (finalCustomerIdsToSearch.isEmpty()){
            return Collections.emptyList();
        }

        Specification<EWaybill> spec = (root, query, cb) -> {
            query.distinct(true);

            // ----- GÜNCELLENMİŞ VE DAHA GÜVENİLİR SORGULAMA MANTIĞI -----
            // 1. Faturalanmış tüm irsaliyelerin ID'lerini çeken bir alt sorgu oluştur.
            Subquery<UUID> invoicedEwaybillsSubquery = query.subquery(UUID.class);
            Root<Invoice> invoiceRoot = invoicedEwaybillsSubquery.from(Invoice.class);
            // 'relatedDespatchesJson' alanını ayrıştırarak ID'leri çekmek yerine,
            // direkt olarak 'invoice_related_e_waybills' join tablosunu kullanacağız.
            // Bu, EWaybill ve Invoice arasında bir @ManyToMany ilişkisi gerektirir.
            // Bu ilişkiyi kurmak yerine, daha basit bir JSONB sorgusu yazalım.
            // NOT: Native Query'ye geçmek bu senaryoda en sağlamı olabilir. Şimdilik Specification ile devam edelim.

            // Mevcut yapımızda doğrudan ilişki olmadığı için, faturalanmış irsaliyeleri Java tarafında bulup
            // ID'lerini sorguya `NOT IN` olarak eklemek daha basit ve güvenilir olacaktır.
            List<UUID> invoicedEwaybillIds = invoiceRepository.findAll().stream()
                    .filter(invoice -> StringUtils.hasText(invoice.getRelatedDespatchesJson()))
                    .flatMap(invoice -> {
                        try {
                            List<Map<String, Object>> despatches = objectMapper.readValue(invoice.getRelatedDespatchesJson(), new TypeReference<>() {});
                            return despatches.stream().map(d -> UUID.fromString((String) d.get("id")));
                        } catch (JsonProcessingException e) {
                            return Stream.empty();
                        }
                    })
                    .collect(Collectors.toList());

            // ----- SORGULAMA MANTIĞI BİTİŞ -----

            List<Predicate> predicates = new ArrayList<>();

            if (!invoicedEwaybillIds.isEmpty()) {
                predicates.add(cb.not(root.get("id").in(invoicedEwaybillIds)));
            }

            predicates.add(root.get("status").in(EWaybillStatus.APPROVED, EWaybillStatus.AWAITING_APPROVAL));
            predicates.add(root.get("customer").get("id").in(finalCustomerIdsToSearch));

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("issueDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("issueDate"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return eWaybillRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "issueDate")).stream()
                .map(ew -> {
                    EWaybillForInvoiceDto dto = new EWaybillForInvoiceDto();
                    dto.setId(ew.getId());
                    dto.setDespatchNumber(ew.getEwaybillNumber());
                    dto.setIssueDate(ew.getIssueDate().atTime(ew.getIssueTime()));
                    dto.setCustomerName(ew.getCustomer().getName());
                    // YENİ KISIM: Kalemleri DTO'ya dönüştürüp ekle
                    List<EWaybillItemResponse> itemDtos = ew.getItems().stream()
                            .map(eWaybillMapper::itemToItemResponseDto)
                            .collect(Collectors.toList());
                    dto.setItems(itemDtos);
                    return dto;
                })
                .collect(Collectors.toList());
    }



    @Transactional(readOnly = true)
    public InvoiceCalculationResponse calculateItemsFromEwaybills(Long customerId, List<UUID> ewaybillIds) {
        InvoiceCalculationResponse response = new InvoiceCalculationResponse();
        Set<String> errors = new HashSet<>();
        Set<String> priceWarnings = new HashSet<>();

        List<EWaybill> ewaybills = eWaybillRepository.findAllById(ewaybillIds);
        if (ewaybills.isEmpty()) {
            throw new IllegalArgumentException("Hesaplama için en az bir geçerli irsaliye seçilmelidir.");
        }

        Customer mainCustomer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        String mainCustomerTaxNumber = Optional.ofNullable(mainCustomer.getTaxInfo())
                .map(TaxInfo::getTaxNumber)
                .orElseThrow(() -> new IllegalStateException("Ana müşterinin vergi bilgisi bulunamadı."));

        for (EWaybill ew : ewaybills) {
            String taxNumber = Optional.ofNullable(ew.getCustomer().getTaxInfo()).map(TaxInfo::getTaxNumber).orElse("");
            if (!mainCustomerTaxNumber.equals(taxNumber)) {
                throw new IllegalArgumentException("Seçilen irsaliyelerden biri (" + ew.getEwaybillNumber() + "), fatura kesilen müşteri ile aynı vergi numarasına sahip değil.");
            }
        }

        Map<Long, CustomerProductAssignment> mainCustomerAssignments = customerProductAssignmentRepository.findByCustomerId(customerId)
                .stream()
                .collect(Collectors.toMap(cpa -> cpa.getProduct().getId(), Function.identity()));

        List<EWaybillItem> allItems = ewaybills.stream().flatMap(ew -> ew.getItems().stream()).collect(Collectors.toList());

        for (EWaybillItem item : allItems) {
            Long productId = item.getProduct().getId();

            if (!mainCustomerAssignments.containsKey(productId)) {
                errors.add(String.format("'%s' ürünü, fatura kesilen ana müşteriye (%s) atanmamış.",
                        item.getProductNameSnapshot(),
                        mainCustomer.getName()
                ));
                continue;
            }

            CustomerProductAssignment assignmentForWarning = mainCustomerAssignments.get(productId);
            LocalDateTime priceUpdateDate = assignmentForWarning.getPriceUpdatedAt();
            LocalDateTime ewaybillIssueDate = item.getEWaybill().getIssueDate().atTime(item.getEWaybill().getIssueTime());

            if (priceUpdateDate != null && priceUpdateDate.isAfter(ewaybillIssueDate)) {
                priceWarnings.add(String.format("'%s' ürünü için fiyat değişti. Etkilenen İrsaliye: %s (%s)",
                        item.getProductNameSnapshot(),
                        item.getEWaybill().getEwaybillNumber(),
                        item.getEWaybill().getCustomer().getName()
                ));
            }
        }

        if (!errors.isEmpty()) {
            response.setItems(new ArrayList<>());
            response.setErrors(new ArrayList<>(errors));
            response.setPriceWarnings(new ArrayList<>(priceWarnings)); // Uyarıları da gönderelim
            return response;
        }

        record ProductPriceKey(Long productId, BigDecimal unitPrice, Integer vatRate) {}

        Map<ProductPriceKey, BigDecimal> groupedItems = allItems.stream()
                .collect(Collectors.groupingBy(
                        item -> new ProductPriceKey(
                                item.getProduct().getId(),
                                item.getPriceVatExclusive(),
                                item.getVatRate()
                        ),
                        Collectors.reducing(BigDecimal.ZERO, EWaybillItem::getQuantity, BigDecimal::add)
                ));

        Map<Long, EWaybillItem> representativeItemMap = allItems.stream()
                .collect(Collectors.toMap(
                        item -> item.getProduct().getId(),
                        Function.identity(),
                        (existing, replacement) -> existing
                ));

        List<CalculatedInvoiceItemDto> calculatedItems = groupedItems.entrySet().stream()
                .map(entry -> {
                    ProductPriceKey key = entry.getKey();
                    BigDecimal totalQuantity = entry.getValue();
                    EWaybillItem representativeItem = representativeItemMap.get(key.productId());
                    CalculatedInvoiceItemDto dto = new CalculatedInvoiceItemDto();
                    dto.setProductId(key.productId());
                    dto.setProductName(representativeItem.getProductNameSnapshot());
                    dto.setQuantity(totalQuantity.intValue());
                    dto.setUnitPrice(key.unitPrice());
                    dto.setVatRate(key.vatRate());
                    return dto;
                })
                .collect(Collectors.toList());

        response.setItems(calculatedItems);
        response.setErrors(new ArrayList<>(errors));
        response.setPriceWarnings(new ArrayList<>(priceWarnings));
        return response;
    }


    private void updateStatusFromTurkcellResponse(Invoice invoice, TurkcellInvoiceStatusResponse response) {
        invoice.setTurkcellStatus(response.getStatus());

        String statusMessage = String.format("Status: %d - %s | Envelope Status: %d - %s",
                response.getStatus(), response.getMessage(),
                response.getEnvelopeStatus(), response.getEnvelopeMessage());

        invoice.setStatusMessage(statusMessage);

        switch (response.getStatus()) {
            case 20: // Kuyruk
            case 30: // Gib'e Gönderiliyor
            case 50: // Gib'e İletildi
                invoice.setStatus(InvoiceStatus.SENDING);
                break;
            case 40: // Hata
            case 62: // Onaylama Hatası
            case 82: // Reddetme Hatası
                invoice.setStatus(InvoiceStatus.REJECTED_BY_GIB);
                break;
            case 60: // Onaylandı
            case 65: // Otomatik Onaylandı
                invoice.setStatus(InvoiceStatus.APPROVED);
                break;
            case 61: // Onaylanıyor
                invoice.setStatus(InvoiceStatus.APPROVING);
                break;
            case 70: // Onay Bekliyor
                invoice.setStatus(InvoiceStatus.AWAITING_APPROVAL);
                break;
            case 80: // Reddedildi
                invoice.setStatus(InvoiceStatus.REJECTED_BY_RECIPIENT);
                break;
            case 81: // Reddediliyor
                invoice.setStatus(InvoiceStatus.REJECTING);
                break;
            case 99: // e-Fatura İptal
                invoice.setStatus(InvoiceStatus.CANCELLED);
                break;
            default:
                log.warn("Invoice {} has unhandled Turkcell status: {}. Keeping current internal status.",
                        invoice.getId(), response.getStatus());
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

    private void validateEwaybillsNotInvoiced(List<UUID> ewaybillIdsToCheck, UUID currentInvoiceIdToExclude) {
        if (CollectionUtils.isEmpty(ewaybillIdsToCheck)) {
            return; // Kontrol edilecek irsaliye yoksa metottan çık
        }

        // Mevcut tüm faturaları çek
        List<Invoice> allInvoices = invoiceRepository.findAll();

        // Daha önce faturalanmış tüm irsaliye ID'lerini bir Set'e topla
        Set<UUID> invoicedEwaybillIds = allInvoices.stream()
                // Eğer bir faturayı güncelliyorsak, o faturanın kendisini bu kontrolden hariç tut
                .filter(invoice -> !invoice.getId().equals(currentInvoiceIdToExclude))
                .filter(invoice -> StringUtils.hasText(invoice.getRelatedDespatchesJson()))
                .flatMap(invoice -> {
                    try {
                        List<Map<String, String>> despatches = objectMapper.readValue(invoice.getRelatedDespatchesJson(), new TypeReference<>() {});
                        return despatches.stream().map(d -> UUID.fromString(d.get("id")));
                    } catch (JsonProcessingException e) {
                        log.error("Could not parse relatedDespatchesJson for invoice {}", invoice.getId(), e);
                        return Stream.empty();
                    }
                })
                .collect(Collectors.toSet());

        // Gelen istekteki irsaliye ID'lerinden herhangi biri, faturalanmışlar listesinde var mı diye kontrol et
        List<UUID> alreadyInvoiced = ewaybillIdsToCheck.stream()
                .filter(invoicedEwaybillIds::contains)
                .collect(Collectors.toList());

        if (!alreadyInvoiced.isEmpty()) {
            // Hangi irsaliyelerin zaten faturalandığını bulup hata mesajına ekle
            String conflictingEwaybillNumbers = eWaybillRepository.findAllById(alreadyInvoiced).stream()
                    .map(EWaybill::getEwaybillNumber)
                    .collect(Collectors.joining(", "));

            throw new IllegalStateException(
                    String.format("Aşağıdaki irsaliyeler zaten başka bir faturaya dahil edilmiş: %s", conflictingEwaybillNumbers)
            );
        }
    }

}