package com.firinyonetim.backend.ewaybill.controller;

import com.firinyonetim.backend.ewaybill.dto.request.BulkEWaybillFromTemplateRequest;
import com.firinyonetim.backend.ewaybill.dto.request.EWaybillCreateRequest;
import com.firinyonetim.backend.ewaybill.dto.response.EWaybillResponse;
import com.firinyonetim.backend.ewaybill.service.EWaybillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/ewaybills")
@RequiredArgsConstructor
// DÜZENLEME: Sınıf seviyesindeki PreAuthorize kaldırıldı.
public class EWaybillController {

    private final EWaybillService eWaybillService;

    @GetMapping
    @PreAuthorize("hasAnyRole('YONETICI', 'DEVELOPER', 'MUHASEBE')")
    public ResponseEntity<List<EWaybillResponse>> getAllEWaybills() {
        return ResponseEntity.ok(eWaybillService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('YONETICI', 'DEVELOPER', 'MUHASEBE')")
    public ResponseEntity<EWaybillResponse> getEWaybillById(@PathVariable UUID id) {
        return ResponseEntity.ok(eWaybillService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('YONETICI', 'DEVELOPER', 'MUHASEBE')")
    public ResponseEntity<EWaybillResponse> createEWaybill(@Valid @RequestBody EWaybillCreateRequest request) {
        log.info("Received EWaybillCreateRequest: {}", request.toString());
        EWaybillResponse response = eWaybillService.createEWaybill(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('YONETICI', 'DEVELOPER', 'MUHASEBE')")
    public ResponseEntity<EWaybillResponse> updateEWaybill(@PathVariable UUID id, @Valid @RequestBody EWaybillCreateRequest request) {
        log.info("Received EWaybillUpdateRequest for id {}: {}", id, request.toString());
        EWaybillResponse response = eWaybillService.updateEWaybill(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('YONETICI', 'DEVELOPER', 'MUHASEBE')")
    public ResponseEntity<Void> deleteEWaybill(@PathVariable UUID id) {
        eWaybillService.deleteEWaybill(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/send")
    @PreAuthorize("hasAnyRole('YONETICI', 'DEVELOPER', 'MUHASEBE')")
    public ResponseEntity<Void> sendEWaybill(@PathVariable UUID id) {
        eWaybillService.sendEWaybill(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('YONETICI', 'DEVELOPER', 'MUHASEBE')")
    public ResponseEntity<byte[]> downloadEWaybillPdf(@PathVariable UUID id) {
        byte[] pdfBytes = eWaybillService.getEWaybillPdf(id);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=e-irsaliye-" + id + ".pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping(value = "/{id}/html", produces = MediaType.TEXT_HTML_VALUE)
    @PreAuthorize("hasAnyRole('YONETICI', 'DEVELOPER', 'MUHASEBE')")
    public ResponseEntity<String> viewEWaybillHtml(@PathVariable UUID id) {
        String htmlContent = eWaybillService.getEWaybillHtml(id);
        return ResponseEntity.ok(htmlContent);
    }

    // YENİ ENDPOINT
    @PostMapping("/create-from-templates")
    @PreAuthorize("hasAnyRole('YONETICI', 'DEVELOPER', 'MUHASEBE')")
    public ResponseEntity<List<EWaybillResponse>> createFromTemplates(@Valid @RequestBody BulkEWaybillFromTemplateRequest request) {
        List<EWaybillResponse> responses = eWaybillService.createEWaybillsFromTemplates(request);
        return new ResponseEntity<>(responses, HttpStatus.CREATED);
    }


}