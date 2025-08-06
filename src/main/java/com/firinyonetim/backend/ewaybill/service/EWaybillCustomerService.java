package com.firinyonetim.backend.ewaybill.service;

import com.firinyonetim.backend.entity.Customer;
import com.firinyonetim.backend.ewaybill.dto.request.EWaybillCustomerInfoRequest;
import com.firinyonetim.backend.ewaybill.dto.response.EWaybillCustomerInfoResponse;
import com.firinyonetim.backend.ewaybill.entity.EWaybillCustomerInfo;
import com.firinyonetim.backend.ewaybill.entity.EWaybillRecipientType;
import com.firinyonetim.backend.ewaybill.mapper.EWaybillCustomerInfoMapper;
import com.firinyonetim.backend.ewaybill.repository.EWaybillCustomerInfoRepository;
import com.firinyonetim.backend.exception.ResourceNotFoundException;
import com.firinyonetim.backend.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EWaybillCustomerService {

    private final EWaybillCustomerInfoRepository infoRepository;
    private final CustomerRepository customerRepository;
    private final EWaybillCustomerInfoMapper infoMapper;

    @Transactional(readOnly = true)
    public EWaybillCustomerInfoResponse getInfoByCustomerId(Long customerId) {
        return infoRepository.findById(customerId)
                .map(infoMapper::toResponse)
                .orElse(null); // Kayıt yoksa null dön
    }

    @Transactional
    public EWaybillCustomerInfoResponse saveOrUpdateInfo(Long customerId, EWaybillCustomerInfoRequest request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        EWaybillCustomerInfo info = infoRepository.findById(customerId)
                .orElse(new EWaybillCustomerInfo());

        info.setCustomer(customer);
        info.setRecipientType(request.getRecipientType());

        if (request.getRecipientType() == EWaybillRecipientType.REGISTERED_USER) {
            if (request.getDefaultAlias() == null || request.getDefaultAlias().isBlank()) {
                throw new IllegalArgumentException("Registered users must have a default alias.");
            }
            info.setDefaultAlias(request.getDefaultAlias());
        } else {
            info.setDefaultAlias(null);
        }

        EWaybillCustomerInfo savedInfo = infoRepository.save(info);
        return infoMapper.toResponse(savedInfo);
    }
}