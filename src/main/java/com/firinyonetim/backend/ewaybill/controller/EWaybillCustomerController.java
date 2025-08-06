package com.firinyonetim.backend.ewaybill.controller;

import com.firinyonetim.backend.ewaybill.dto.request.EWaybillCustomerInfoRequest;
import com.firinyonetim.backend.ewaybill.dto.response.EWaybillCustomerInfoResponse;
import com.firinyonetim.backend.ewaybill.service.EWaybillCustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ewaybill-customer-info")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('YONETICI', 'DEVELOPER', 'MUHASEBE')")
public class EWaybillCustomerController {

    private final EWaybillCustomerService customerService;

    @GetMapping("/{customerId}")
    public ResponseEntity<EWaybillCustomerInfoResponse> getInfo(@PathVariable Long customerId) {
        EWaybillCustomerInfoResponse response = customerService.getInfoByCustomerId(customerId);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{customerId}")
    public ResponseEntity<EWaybillCustomerInfoResponse> saveOrUpdateInfo(@PathVariable Long customerId, @Valid @RequestBody EWaybillCustomerInfoRequest request) {
        return ResponseEntity.ok(customerService.saveOrUpdateInfo(customerId, request));
    }

    // YENİ ENDPOINT
    @PostMapping("/{customerId}/query-gib")
    public ResponseEntity<List<String>> queryGib(@PathVariable Long customerId) {
        return ResponseEntity.ok(customerService.queryGibAndSaveInfo(customerId));
    }

}