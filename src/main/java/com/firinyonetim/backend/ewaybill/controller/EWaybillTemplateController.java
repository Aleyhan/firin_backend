// src/main/java/com/firinyonetim/backend/ewaybill/controller/EWaybillTemplateController.java
package com.firinyonetim.backend.ewaybill.controller;

import com.firinyonetim.backend.ewaybill.dto.request.EWaybillTemplateRequest;
import com.firinyonetim.backend.ewaybill.dto.response.EWaybillTemplateResponse;
import com.firinyonetim.backend.ewaybill.service.EWaybillTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers/{customerId}/ewaybill-template")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('YONETICI', 'DEVELOPER', 'MUHASEBE')")
public class EWaybillTemplateController {

    private final EWaybillTemplateService templateService;

    @GetMapping
    public ResponseEntity<EWaybillTemplateResponse> getTemplate(@PathVariable Long customerId) {
        return ResponseEntity.ok(templateService.getTemplateByCustomerId(customerId));
    }

    @PostMapping
    public ResponseEntity<EWaybillTemplateResponse> createTemplate(
            @PathVariable Long customerId,
            @Valid @RequestBody EWaybillTemplateRequest request) {
        return new ResponseEntity<>(templateService.createTemplate(customerId, request), HttpStatus.CREATED);
    }

    @PutMapping
    public ResponseEntity<EWaybillTemplateResponse> updateTemplate(
            @PathVariable Long customerId,
            @Valid @RequestBody EWaybillTemplateRequest request) {
        return ResponseEntity.ok(templateService.updateTemplate(customerId, request));
    }

    // YENİ ENDPOINT
    @DeleteMapping
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long customerId) {
        templateService.deleteTemplate(customerId);
        return ResponseEntity.noContent().build();
    }

}