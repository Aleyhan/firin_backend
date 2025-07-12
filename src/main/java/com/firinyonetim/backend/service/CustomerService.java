package com.firinyonetim.backend.service;

import com.firinyonetim.backend.dto.customer.request.CustomerCreateRequest;
import com.firinyonetim.backend.dto.customer.request.CustomerUpdateRequest;
import com.firinyonetim.backend.dto.customer.response.CustomerResponse;
import com.firinyonetim.backend.dto.special_price.request.SpecialPriceRequest;
import com.firinyonetim.backend.entity.*;
import com.firinyonetim.backend.exception.ResourceNotFoundException;
import com.firinyonetim.backend.mapper.CustomerMapper;
import com.firinyonetim.backend.repository.CustomerRepository;
import com.firinyonetim.backend.repository.ProductRepository;
import com.firinyonetim.backend.repository.SpecialProductPriceRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final SpecialProductPriceRepository specialPriceRepository;
    private final CustomerMapper customerMapper;

    public List<CustomerResponse> getAllCustomers() {
        return customerRepository.findAll().stream()
                .map(customerMapper::toCustomerResponse)
                .collect(Collectors.toList());
    }

    public CustomerResponse getCustomerById(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found with id: " + customerId));
        return customerMapper.toCustomerResponse(customer);
    }

    @Transactional
    public CustomerResponse createCustomer(CustomerCreateRequest request) {

        if (customerRepository.existsByCustomerCode(request.getCustomerCode())) {
            throw new IllegalStateException("Customer code " + request.getCustomerCode() + " is already in use.");
        }
        // 1. Gelen DTO'yu ana Customer entity'sine çevir.
        // CustomerMapper, içindeki TaxInfoRequest ve AddressRequest'leri de çevirecektir.
        Customer customer = customerMapper.toCustomer(request);

        // 2. Çift yönlü ilişkileri manuel olarak kur.
        // Adreslerin customer referansını set et.
        if (customer.getAddresses() != null) {
            customer.getAddresses().forEach(address -> address.setCustomer(customer));
        }

        // Vergi bilgisinin customer referansını set et.
        if (customer.getTaxInfo() != null) {
            customer.getTaxInfo().setCustomer(customer);
        }

        // 3. Müşteriyi kaydet. Cascade ayarları sayesinde ilişkili tüm varlıklar da kaydedilecek.
        Customer savedCustomer = customerRepository.save(customer);

        // 4. Kaydedilen entity'i response DTO'suna çevirip döndür.
        return customerMapper.toCustomerResponse(savedCustomer);
    }

    @Transactional
    public CustomerResponse addOrUpdateSpecialPrice(Long customerId, SpecialPriceRequest request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found with id: " + customerId));
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + request.getProductId()));

        SpecialProductPrice specialPrice = specialPriceRepository
                .findByCustomerIdAndProductId(customerId, request.getProductId())
                .map(existingPrice -> {
                    existingPrice.setPrice(request.getPrice());
                    return existingPrice;
                })
                .orElseGet(() -> {
                    SpecialProductPrice newPrice = new SpecialProductPrice();
                    newPrice.setCustomer(customer);
                    newPrice.setProduct(product);
                    newPrice.setPrice(request.getPrice());
                    customer.getSpecialPrices().add(newPrice);
                    return newPrice;
                });

        specialPriceRepository.save(specialPrice);

        return customerMapper.toCustomerResponse(customer);
    }

// ... CustomerService sınıfının içinde ...

    @Transactional
    public CustomerResponse updateCustomer(Long customerId, CustomerUpdateRequest request) {
        // 1. Güncellenecek müşteriyi veritabanından bul
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        // 2. Müşterinin temel alanlarını güncelle
        // customerCode'un güncellenmesine izin vermiyoruz.
        customer.setName(request.getName());
        customer.setPhone(request.getPhone());
        customer.setEmail(request.getEmail());
        customer.setActive(request.getIsActive());
        customer.setNotes(request.getNotes()); // Notes alanını güncellemeyi ekleyelim.

        // 3. Vergi Bilgisini Güvenli Bir Şekilde Güncelle/Oluştur
        if (request.getTaxInfo() != null) {
            TaxInfo taxInfo = customer.getTaxInfo();
            if (taxInfo == null) {
                // Eğer müşterinin daha önce vergi bilgisi yoksa, yeni bir tane oluştur
                taxInfo = new TaxInfo();
                taxInfo.setCustomer(customer);
                customer.setTaxInfo(taxInfo);
            }
            // Alanları güncelle
            taxInfo.setTradeName(request.getTaxInfo().getTradeName());
            taxInfo.setTaxNumber(request.getTaxInfo().getTaxNumber());
            taxInfo.setTaxOffice(request.getTaxInfo().getTaxOffice());
        } else if (customer.getTaxInfo() != null) {
            // Eğer request'te taxInfo null gelirse ve müşterinin vergi bilgisi varsa, onu sil.
            customer.setTaxInfo(null);
        }

        // 4. Adresleri Güvenli Bir Şekilde Güncelle/Ekle/Sil
        // Önce mevcut adres listesini temizle. orphanRemoval=true sayesinde
        // listeden çıkarılan adresler veritabanından silinecek.
        if (customer.getAddresses() != null) {
            customer.getAddresses().clear();
        } else {
            // Eğer adres listesi null ise, boş bir liste oluştur.
            customer.setAddresses(new ArrayList<>());
        }

        // Gelen yeni adresleri ekle (eğer varsa)
        if (request.getAddresses() != null && !request.getAddresses().isEmpty()) {
            request.getAddresses().forEach(addressReq -> {
                Address newAddress = new Address();
                newAddress.setDetails(addressReq.getDetails());
                newAddress.setProvince(addressReq.getProvince());
                newAddress.setDistrict(addressReq.getDistrict());
                newAddress.setCustomer(customer); // İlişkiyi kur
                customer.getAddresses().add(newAddress); // Müşterinin listesine ekle
            });
        }

        // 5. Değişiklikleri kaydet. @Transactional sayesinde tüm değişiklikler tek seferde işlenir.
        Customer updatedCustomer = customerRepository.save(customer);

        return customerMapper.toCustomerResponse(updatedCustomer);
    }

    // ... CustomerService sınıfının içinde ...

    @Transactional
    public void deleteCustomer(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));
        customer.setActive(false);
        customerRepository.save(customer);
    }

    @Transactional
    public void removeSpecialPrice(Long customerId, Long productId) {
        // Önce varlıkların olup olmadığını kontrol et
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer not found with id: " + customerId);
        }
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found with id: " + productId);
        }
        specialPriceRepository.deleteByCustomerIdAndProductId(customerId, productId);
    }

    @Transactional
    public CustomerResponse updateCustomerFields(Long customerId, Map<String, Object> updates) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        updates.forEach((key, value) -> {
            switch (key) {
                case "name":
                    customer.setName((String) value);
                    break;
                case "phone":
                    customer.setPhone((String) value);
                    break;
                case "email":
                    customer.setEmail((String) value);
                    break;
                case "isActive":
                    customer.setActive((Boolean) value);
                    break;
                case "notes":
                    customer.setNotes((String) value);
                    break;
                // Do not allow customerCode updates
            }
        });

        Customer updatedCustomer = customerRepository.save(customer);
        return customerMapper.toCustomerResponse(updatedCustomer);
    }
}