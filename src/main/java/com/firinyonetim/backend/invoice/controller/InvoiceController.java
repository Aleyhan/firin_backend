package com.firinyonetim.backend.invoice.controller;

import com.firinyonetim.backend.dto.PagedResponseDto;
import com.firinyonetim.backend.invoice.dto.InvoiceCreateRequest;
import com.firinyonetim.backend.invoice.dto.InvoiceResponse;
import com.firinyonetim.backend.invoice.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
}