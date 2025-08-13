package com.firinyonetim.backend.invoice.controller;

import com.firinyonetim.backend.dto.PagedResponseDto;
import com.firinyonetim.backend.invoice.dto.*;
import com.firinyonetim.backend.invoice.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate; // YENİ IMPORT

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('YONETICI', 'DEVELOPER', 'MUHASEBE')")
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping
    public ResponseEntity<PagedResponseDto<InvoiceResponse>> getAllInvoices(
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return ResponseEntity.ok(invoiceService.getAllInvoices(status, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> getInvoiceById(@PathVariable UUID id) {
        return ResponseEntity.ok(invoiceService.getInvoiceById(id));
    }

    @PostMapping
    public ResponseEntity<InvoiceResponse> createDraftInvoice(@Valid @RequestBody InvoiceCreateRequest request) {
        return new ResponseEntity<>(invoiceService.createDraftInvoice(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<InvoiceResponse> updateDraftInvoice(@PathVariable UUID id, @Valid @RequestBody InvoiceCreateRequest request) {
        return ResponseEntity.ok(invoiceService.updateDraftInvoice(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDraftInvoice(@PathVariable UUID id) {
        invoiceService.deleteDraftInvoice(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/send")
    public ResponseEntity<InvoiceResponse> sendInvoice(@PathVariable UUID id) {
        return ResponseEntity.ok(invoiceService.sendInvoice(id));
    }

    // YENİ ENDPOINT
    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable UUID id) {
        byte[] pdfBytes = invoiceService.getInvoicePdf(id);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=e-fatura-" + id + ".pdf");
        return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_PDF).body(pdfBytes);
    }

    // YENİ ENDPOINT
    @GetMapping(value = "/{id}/html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> viewInvoiceHtml(@PathVariable UUID id) {
        String htmlContent = invoiceService.getInvoiceHtml(id);
        return ResponseEntity.ok(htmlContent);
    }

    // YENİ ENDPOINT: Gelişmiş filtreleme ile faturalanmamış irsaliyeleri bul
    @GetMapping("/find-unvoiced-ewaybills")
    public ResponseEntity<List<EWaybillForInvoiceDto>> findUninvoicedEWaybills(
            @RequestParam Long customerId, // Faturanın kesileceği ana müşteri
            @RequestParam(required = false) List<Long> searchCustomerIds, // Filtrelenecek müşteri ID'leri
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(invoiceService.findUninvoicedEWaybills(customerId, searchCustomerIds, startDate, endDate));
    }

    // BU METODU BUL VE DÖNÜŞ TİPİNİ GÜNCELLE
    @GetMapping("/calculate-items-from-ewaybills")
    public ResponseEntity<InvoiceCalculationResponse> calculateItemsFromEwaybills(
            @RequestParam Long customerId,
            @RequestParam List<UUID> ewaybillIds) {
        return ResponseEntity.ok(invoiceService.calculateItemsFromEwaybills(customerId, ewaybillIds));
    }

    // YENİ ENDPOINT
    @PostMapping("/check-statuses")
    public ResponseEntity<Void> checkStatuses() {
        invoiceService.checkAndUpdateStatuses();
        return ResponseEntity.ok().build();
    }


}