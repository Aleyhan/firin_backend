// src/main/java/com/firinyonetim/backend/ewaybill/service/EWaybillTemplateService.java
package com.firinyonetim.backend.ewaybill.service;

import com.firinyonetim.backend.entity.Customer;
import com.firinyonetim.backend.entity.Product;
import com.firinyonetim.backend.entity.User;
import com.firinyonetim.backend.ewaybill.dto.request.EWaybillTemplateItemRequest;
import com.firinyonetim.backend.ewaybill.dto.request.EWaybillTemplateRequest;
import com.firinyonetim.backend.ewaybill.dto.response.EWaybillTemplateResponse;
import com.firinyonetim.backend.ewaybill.entity.EWaybillTemplate;
import com.firinyonetim.backend.ewaybill.entity.EWaybillTemplateItem;
import com.firinyonetim.backend.ewaybill.mapper.EWaybillTemplateMapper;
import com.firinyonetim.backend.ewaybill.repository.EWaybillTemplateRepository;
import com.firinyonetim.backend.exception.ResourceNotFoundException;
import com.firinyonetim.backend.repository.CustomerRepository;
import com.firinyonetim.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class EWaybillTemplateService {

    private final EWaybillTemplateRepository templateRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final EWaybillTemplateMapper templateMapper;

    @Transactional(readOnly = true)
    public EWaybillTemplateResponse getTemplateByCustomerId(Long customerId) {
        EWaybillTemplate template = templateRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("E-Waybill template not found for customer id: " + customerId));
        return templateMapper.toResponse(template);
    }

    @Transactional
    public EWaybillTemplateResponse createTemplate(Long customerId, EWaybillTemplateRequest request) {
        if (templateRepository.existsByCustomerId(customerId)) {
            throw new IllegalStateException("Customer with id " + customerId + " already has a template. Use PUT to update.");
        }
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        EWaybillTemplate template = new EWaybillTemplate();
        template.setCustomer(customer);

        return mapAndSave(template, request, currentUser);
    }

    @Transactional
    public EWaybillTemplateResponse updateTemplate(Long customerId, EWaybillTemplateRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        EWaybillTemplate template = templateRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("E-Waybill template not found for customer id: " + customerId));

        return mapAndSave(template, request, currentUser);
    }

    private EWaybillTemplateResponse mapAndSave(EWaybillTemplate template, EWaybillTemplateRequest request, User currentUser) {
        templateMapper.updateFromRequest(request, template);
        template.setLastUpdatedBy(currentUser);

        // --- YENİ: includedFields koleksiyonunu manuel olarak yönet ---
        template.getIncludedFields().clear();
        if (request.getIncludedFields() != null) {
            template.getIncludedFields().addAll(request.getIncludedFields());
        }
        // --- YENİ KOD SONU ---

        template.getItems().clear();

        processTemplateItems(request.getItems(), template);
        calculateAndSetTotals(template);

        EWaybillTemplate savedTemplate = templateRepository.save(template);
        log.info("E-Waybill template for customer {} saved/updated by user {}", template.getCustomer().getId(), currentUser.getUsername());
        return templateMapper.toResponse(savedTemplate);
    }

    private void processTemplateItems(Set<EWaybillTemplateItemRequest> itemRequests, EWaybillTemplate template) {
        for (EWaybillTemplateItemRequest itemDto : itemRequests) {
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + itemDto.getProductId()));

            EWaybillTemplateItem item = new EWaybillTemplateItem();
            item.setProduct(product);
            item.setProductNameSnapshot(product.getName());
            item.setQuantity(itemDto.getQuantity());
            item.setUnitPrice(itemDto.getUnitPrice());

            String unitCode = "C62";
            if (product.getUnit() != null && StringUtils.hasText(product.getUnit().getCode())) {
                unitCode = product.getUnit().getCode();
            }
            item.setUnitCode(unitCode);

            BigDecimal lineAmount = itemDto.getQuantity().multiply(itemDto.getUnitPrice());
            item.setLineAmount(lineAmount.setScale(2, RoundingMode.HALF_UP));

            item.setVatRate(product.getVatRate());
            BigDecimal vatAmount = lineAmount.multiply(BigDecimal.valueOf(product.getVatRate())).divide(new BigDecimal(100));
            item.setVatAmount(vatAmount.setScale(2, RoundingMode.HALF_UP));

            template.addItem(item);
        }
    }

    private void calculateAndSetTotals(EWaybillTemplate template) {
        BigDecimal totalAmountWithoutVat = BigDecimal.ZERO;
        BigDecimal totalVatAmount = BigDecimal.ZERO;

        for (EWaybillTemplateItem item : template.getItems()) {
            totalAmountWithoutVat = totalAmountWithoutVat.add(item.getLineAmount());
            totalVatAmount = totalVatAmount.add(item.getVatAmount());
        }

        template.setTotalAmountWithoutVat(totalAmountWithoutVat.setScale(2, RoundingMode.HALF_UP));
        template.setTotalVatAmount(totalVatAmount.setScale(2, RoundingMode.HALF_UP));
        template.setTotalAmountWithVat(totalAmountWithoutVat.add(totalVatAmount).setScale(2, RoundingMode.HALF_UP));
    }
}