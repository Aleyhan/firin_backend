package com.firinyonetim.backend.invoice.service;

import com.firinyonetim.backend.dto.PagedResponseDto;
import com.firinyonetim.backend.entity.Customer;
import com.firinyonetim.backend.entity.Product;
import com.firinyonetim.backend.entity.User;
import com.firinyonetim.backend.exception.ResourceNotFoundException;
import com.firinyonetim.backend.invoice.dto.InvoiceCreateRequest;
import com.firinyonetim.backend.invoice.dto.InvoiceItemRequest;
import com.firinyonetim.backend.invoice.dto.InvoiceResponse;
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
import com.firinyonetim.backend.repository.CustomerRepository;
import com.firinyonetim.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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

        // DEĞİŞİKLİK BURADA: Koleksiyonu set etmek yerine, mevcut koleksiyonu temizleyip yenilerini ekliyoruz.
        invoice.getItems().clear();
        Set<InvoiceItem> newItems = processInvoiceItems(request.getItems(), invoice);
        invoice.getItems().addAll(newItems);

        calculateTotals(invoice);

        Invoice updatedInvoice = invoiceRepository.save(invoice);
        log.info("Draft invoice updated with id: {}", updatedInvoice.getId());
        return invoiceMapper.toResponse(updatedInvoice);
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

    private void updateStatusFromTurkcellResponse(Invoice invoice, TurkcellInvoiceStatusResponse response) {
        invoice.setTurkcellStatus(response.getStatus());
        invoice.setStatusMessage(response.getMessage());

        switch (response.getStatus()) {
            case 60: // Onaylandı
                invoice.setStatus(InvoiceStatus.APPROVED);
                break;
            case 40: // Hata
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

            BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())).subtract(item.getDiscountAmount());
            item.setTotalPrice(lineTotal);

            BigDecimal vatRate = BigDecimal.valueOf(product.getVatRate()).divide(BigDecimal.valueOf(100));
            BigDecimal vatAmount = lineTotal.multiply(vatRate).setScale(2, RoundingMode.HALF_UP);
            item.setVatRate(product.getVatRate());
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