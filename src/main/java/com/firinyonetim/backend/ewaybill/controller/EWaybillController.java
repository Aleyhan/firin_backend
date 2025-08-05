package com.firinyonetim.backend.ewaybill.controller;

import com.firinyonetim.backend.ewaybill.dto.request.EWaybillCreateRequest;
import com.firinyonetim.backend.ewaybill.dto.response.EWaybillResponse;
import com.firinyonetim.backend.ewaybill.service.EWaybillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders; // YENİ IMPORT


import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ewaybills")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('YONETICI', 'DEVELOPER', 'MUHASEBE')")
public class EWaybillController {

    private final EWaybillService eWaybillService;

    @GetMapping
    public ResponseEntity<List<EWaybillResponse>> getAllEWaybills() {
        return ResponseEntity.ok(eWaybillService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EWaybillResponse> getEWaybillById(@PathVariable UUID id) {
        return ResponseEntity.ok(eWaybillService.findById(id));
    }

    @PostMapping
    public ResponseEntity<EWaybillResponse> createEWaybill(@Valid @RequestBody EWaybillCreateRequest request) {
        EWaybillResponse response = eWaybillService.createEWaybill(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EWaybillResponse> updateEWaybill(@PathVariable UUID id, @Valid @RequestBody EWaybillCreateRequest request) {
        EWaybillResponse response = eWaybillService.updateEWaybill(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEWaybill(@PathVariable UUID id) {
        eWaybillService.deleteEWaybill(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/send")
    public ResponseEntity<Void> sendEWaybill(@PathVariable UUID id) {
        eWaybillService.sendEWaybill(id);
        return ResponseEntity.ok().build();
    }

    // YENİ ENDPOINT
    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadEWaybillPdf(@PathVariable UUID id) {
        byte[] pdfBytes = eWaybillService.getEWaybillPdf(id);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=e-irsaliye-" + id + ".pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    // YENİ ENDPOINT
    @GetMapping(value = "/{id}/html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> viewEWaybillHtml(@PathVariable UUID id) {
        String htmlContent = eWaybillService.getEWaybillHtml(id);
        return ResponseEntity.ok(htmlContent);
    }
}
