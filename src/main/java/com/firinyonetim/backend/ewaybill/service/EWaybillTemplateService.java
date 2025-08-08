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

import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class EWaybillTemplateService {

    private final EWaybillTemplateRepository templateRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final EWaybillTemplateMapper templateMapper;

    // --- DEĞİŞİKLİK BURADA BAŞLIYOR ---
    @Transactional(readOnly = true)
    public Optional<EWaybillTemplateResponse> getTemplateByCustomerId(Long customerId) {
        // Artık ResourceNotFoundException fırlatmıyor.
        // Bunun yerine Optional<EWaybillTemplate> bulup, varsa DTO'ya map'liyor.
        return templateRepository.findByCustomerId(customerId)
                .map(templateMapper::toResponse);
    }
    // --- DEĞİŞİKLİK BURADA BİTİYOR ---


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

        template.getItems().clear();

        processTemplateItems(request.getItems(), template);

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

            String unitCode = "C62";
            if (product.getUnit() != null && StringUtils.hasText(product.getUnit().getCode())) {
                unitCode = product.getUnit().getCode();
            }
            item.setUnitCode(unitCode);

            template.addItem(item);
        }
    }

    @Transactional
    public void deleteTemplate(Long customerId) {
        if (!templateRepository.existsByCustomerId(customerId)) {
            throw new ResourceNotFoundException("Template not found for customer id: " + customerId);
        }
        // DÜZELTME: Daha basit ve doğru silme metodu
        templateRepository.deleteByCustomerId(customerId);
        log.info("E-Waybill template for customer {} deleted.", customerId);
    }

}