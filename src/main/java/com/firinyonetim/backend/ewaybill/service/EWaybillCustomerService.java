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
import com.firinyonetim.backend.ewaybill.dto.turkcell.GibUser; // YENİ
import org.springframework.util.StringUtils; // YENİ IMPORT

import java.util.Collections;
import java.util.List; // YENİ
import java.util.stream.Collectors; // YENİ

@Service
@RequiredArgsConstructor
public class EWaybillCustomerService {

    private final EWaybillCustomerInfoRepository infoRepository;
    private final CustomerRepository customerRepository;
    private final EWaybillCustomerInfoMapper infoMapper;
    private final TurkcellEWaybillClient turkcellClient; // YENİ


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

    @Transactional
    public List<String> queryGibAndSaveInfo(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        if (customer.getTaxInfo() == null || !StringUtils.hasText(customer.getTaxInfo().getTaxNumber())) {
            throw new IllegalStateException("Customer does not have a tax number (VKN/TCKN).");
        }

        String vknTckn = customer.getTaxInfo().getTaxNumber();
        List<GibUser> allUsers = turkcellClient.getGibUserList();

        // DÜZENLEME: Filtrelemeye appType == 3 koşulu eklendi.
        List<String> foundAliases = allUsers.stream()
                .filter(user -> vknTckn.equals(user.getIdentifier()) && user.getAppType() == 3)
                .map(GibUser::getAlias)
                .collect(Collectors.toList());

        EWaybillCustomerInfo info = infoRepository.findById(customerId).orElse(new EWaybillCustomerInfo());
        info.setCustomer(customer);

        if (foundAliases.isEmpty()) {
            // Mükellef değil veya e-İrsaliye kaydı yok
            info.setRecipientType(EWaybillRecipientType.NOT_REGISTERED);
            info.setDefaultAlias(null);
            infoRepository.save(info);
            return Collections.emptyList();
        } else if (foundAliases.size() == 1) {
            // Tek e-İrsaliye alias'ı var
            info.setRecipientType(EWaybillRecipientType.REGISTERED_USER);
            info.setDefaultAlias(foundAliases.get(0));
            infoRepository.save(info);
            return foundAliases;
        } else {
            // Birden fazla e-İrsaliye alias'ı var
            return foundAliases;
        }
    }

}