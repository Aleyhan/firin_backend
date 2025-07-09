package com.firinyonetim.backend.service;

import com.firinyonetim.backend.dto.customer.request.CustomerCreateRequest;
import com.firinyonetim.backend.dto.customer.response.CustomerResponse;
import com.firinyonetim.backend.dto.special_price.request.SpecialPriceRequest;
import com.firinyonetim.backend.entity.Customer;
import com.firinyonetim.backend.entity.Product;
import com.firinyonetim.backend.entity.SpecialProductPrice;
import com.firinyonetim.backend.mapper.CustomerMapper;
import com.firinyonetim.backend.repository.CustomerRepository;
import com.firinyonetim.backend.repository.ProductRepository;
import com.firinyonetim.backend.repository.SpecialProductPriceRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final SpecialProductPriceRepository specialPriceRepository;
    private final CustomerMapper customerMapper;

    // --- Diğer Metodlar (Listeleme vb. ekleyelim) ---
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

    // --- Düzeltilmiş Metodlar ---

    @Transactional
    public CustomerResponse createCustomer(CustomerCreateRequest request) {
        Customer customer = customerMapper.toCustomer(request);
        // Adreslerin customer referansını set et (Çift yönlü ilişkiyi kur)
        if (customer.getAddresses() != null) {
            customer.getAddresses().forEach(address -> address.setCustomer(customer));
        }
        Customer savedCustomer = customerRepository.save(customer);
        return customerMapper.toCustomerResponse(savedCustomer);
    }

    @Transactional
    public CustomerResponse addOrUpdateSpecialPrice(Long customerId, SpecialPriceRequest request) {
        // 1. Gerekli varlıkları bul
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found with id: " + customerId));
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + request.getProductId()));

        // 2. Mevcut özel fiyatı bul veya yeni oluştur
        SpecialProductPrice specialPrice = specialPriceRepository
                .findByCustomerIdAndProductId(customerId, request.getProductId())
                .map(existingPrice -> {
                    // Varsa, sadece fiyatı güncelle
                    existingPrice.setPrice(request.getPrice());
                    return existingPrice;
                })
                .orElseGet(() -> {
                    // Yoksa, yeni bir tane oluştur ve ilişkileri kur
                    SpecialProductPrice newPrice = new SpecialProductPrice();
                    newPrice.setCustomer(customer);
                    newPrice.setProduct(product);
                    newPrice.setPrice(request.getPrice());
                    // DÜZELTME 1: İlişkinin her iki tarafını da senkronize et
                    customer.getSpecialPrices().add(newPrice);
                    return newPrice;
                });

        // 3. Değişikliği kaydet (Yeni veya güncellenmiş)
        specialPriceRepository.save(specialPrice);

        // DÜZELTME 2: Gereksiz veritabanı sorgusunu kaldır.
        // Bellekteki güncel 'customer' nesnesini doğrudan DTO'ya çevir ve döndür.
        return customerMapper.toCustomerResponse(customer);
    }
}