package com.firinyonetim.backend.service;

import com.firinyonetim.backend.dto.address.request.AddressRequest;
import com.firinyonetim.backend.dto.customer.request.CustomerCreateRequest;
import com.firinyonetim.backend.dto.customer.request.CustomerUpdateRequest;
import com.firinyonetim.backend.dto.customer.response.CustomerResponse;
import com.firinyonetim.backend.dto.customer.response.LastPaymentDateResponse;
import com.firinyonetim.backend.dto.tax_info.request.TaxInfoRequest;
import com.firinyonetim.backend.entity.*;
import com.firinyonetim.backend.exception.ResourceNotFoundException;
import com.firinyonetim.backend.mapper.AddressMapper;
import com.firinyonetim.backend.mapper.CustomerMapper;
import com.firinyonetim.backend.mapper.ProductMapper;
import com.firinyonetim.backend.mapper.TaxInfoMapper;
import com.firinyonetim.backend.repository.CustomerRepository;
import com.firinyonetim.backend.repository.ProductRepository;
import com.firinyonetim.backend.repository.RouteRepository;
import com.firinyonetim.backend.repository.RouteAssignmentRepository;
import com.firinyonetim.backend.repository.TaxInfoRepository;
import com.firinyonetim.backend.repository.TransactionPaymentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final CustomerMapper customerMapper;
    private final ProductMapper productMapper;
    private final TaxInfoRepository taxInfoRepository;
    private final TaxInfoMapper taxInfoMapper;
    private final AddressMapper addressMapper; // Yeni eklenen AddressMapper
    private final TransactionPaymentRepository transactionPaymentRepository;
    private final RouteRepository routeRepository;
    private final RouteAssignmentRepository routeAssignmentRepository;


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

// --- DEBUG İÇİN BU SATIRI EKLEYİN ---
        System.out.println("Request DTO workingDays: " + request.getWorkingDays());
        System.out.println("Mapped Entity workingDays: " + customer.getWorkingDays());
        // --- DEBUG SONU ---

        // Vergi bilgisinin customer referansını set et.
        if (customer.getTaxInfo() != null) {
            customer.getTaxInfo().setCustomer(customer);
        }

        // --- DÜZELTME BURADA ---
        // workingDays koleksiyonunu manuel olarak DTO'dan alıp entity'e set ediyoruz.
        // Bu, mapper'ın eksik bıraktığı işi tamamlar.
        if (request.getWorkingDays() != null) {
            customer.setWorkingDays(new HashSet<>(request.getWorkingDays()));
        }
        // --- DÜZELTME SONU ---
        System.out.println("Before Save - Entity workingDays: " + customer.getWorkingDays());

        if (customer.getTaxInfo() != null) {
            customer.getTaxInfo().setCustomer(customer);
        }

        // 3. Müşteriyi kaydet. Cascade ayarları sayesinde ilişkili tüm varlıklar da kaydedilecek.
        Customer savedCustomer = customerRepository.save(customer);

        System.out.println("After Save - Saved Entity workingDays: " + savedCustomer.getWorkingDays());

        CustomerResponse response = customerMapper.toCustomerResponse(savedCustomer);

        // --- DEBUG 3 ---
        System.out.println("After Mapping to Response - Response DTO workingDays: " + response.getWorkingDays());

        return response;
    }

    public LastPaymentDateResponse getLastPaymentDate(Long customerId) {
        return transactionPaymentRepository.findLastPaymentDateByCustomerId(customerId)
                .map(dateTime -> new LastPaymentDateResponse(true, dateTime.toLocalDate()))
                .orElse(new LastPaymentDateResponse(false, null));
    }


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
        // 4. ADRESİ GÜNCELLEME (YENİ YAKLAŞIM)
        if (request.getAddress() != null) {
            Address address = customer.getAddress();
            if (address == null) {
                // Müşterinin adresi yoksa, yeni bir tane oluştur.
                address = new Address();
                customer.setAddress(address);
            }
            // Alanları güncelle
            address.setDetails(request.getAddress().getDetails());
            address.setProvince(request.getAddress().getProvince());
            address.setDistrict(request.getAddress().getDistrict());
        } else {
            // Eğer request'te adres null gelirse, müşterinin mevcut adresini sil.
            customer.setAddress(null);
        }

        if (request.getWorkingDays() != null) {
            // Manuel olarak DTO'dan gelen set'i atıyoruz.
            // Bu, 'clear' ve 'addAll' yapmaktan daha güvenilir olabilir.
            customer.setWorkingDays(new HashSet<>(request.getWorkingDays()));
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
                        case "customerCode":
                            if (value == null || !(value instanceof String)) {
                                throw new IllegalArgumentException("Müşteri kodu metin formatında olmalıdır.");
                            }
                            String newCode = (String) value;

                            // 1. Uzunluk Kontrolü
                            if (newCode.length() != 4) {
                                throw new IllegalArgumentException("Müşteri kodu tam olarak 4 haneli olmalıdır.");
                            }

                            // 2. Benzersizlik (Unique) Kontrolü
                            // Yeni kodun, mevcut müşteri dışındaki başka bir müşteriye ait olup olmadığını kontrol et
                            if (customerRepository.existsByCustomerCodeAndIdNot(newCode, customer.getId())) {
                                throw new IllegalStateException("Müşteri kodu '" + newCode + "' zaten başka bir müşteri tarafından kullanılıyor.");
                            }

                            // Validasyonlar başarılıysa, kodu güncelle
                            customer.setCustomerCode(newCode);
                            break;
                        // YENİ EKLENEN BLOK SONU
                    }
                }
        );

        Customer updatedCustomer = customerRepository.save(customer);
        return customerMapper.toCustomerResponse(updatedCustomer);
    }

    // YENİ METOT
    @Transactional
    public CustomerResponse updateCustomerTaxInfo(Long customerId, Map<String, String> updates) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        // Müşterinin mevcut vergi bilgisini al veya yoksa yeni bir tane oluştur.
        TaxInfo taxInfo = customer.getTaxInfo();
        if (taxInfo == null) {
            // Eğer request'te en az bir alan varsa yeni TaxInfo oluştur.
            if (updates == null || updates.isEmpty()) {
                // Güncellenecek bir şey yoksa, müşteriyi olduğu gibi döndür.
                return customerMapper.toCustomerResponse(customer);
            }
            taxInfo = new TaxInfo();
            taxInfo.setCustomer(customer);
            customer.setTaxInfo(taxInfo);
        }

        final TaxInfo finalTaxInfo = taxInfo; // Lambda içinde kullanmak için

        updates.forEach((key, value) -> {
            // Değerin boş veya null olmamasını kontrol edelim.
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalArgumentException("'" + key + "' alanı boş olamaz.");
            }

            switch (key) {
                case "tradeName":
                    finalTaxInfo.setTradeName(value);
                    break;
                case "taxOffice":
                    finalTaxInfo.setTaxOffice(value);
                    break;
                case "taxNumber":
                    // Benzersizlik kontrolü
                    if (taxInfoRepository.existsByTaxNumberAndCustomerIdNot(value, customerId)) {
                        throw new IllegalStateException("Vergi numarası '" + value + "' zaten başka bir müşteri tarafından kullanılıyor.");
                    }
                    finalTaxInfo.setTaxNumber(value);
                    break;
                default:
                    // Bilinmeyen bir anahtar gelirse hata fırlatabilir veya görmezden gelebiliriz.
                    // Şimdilik görmezden gelelim.
                    break;
            }
        });

        // TaxInfo'nun kaydedilmesi için ana Customer nesnesini kaydetmek yeterlidir (Cascade ayarı sayesinde).
        Customer updatedCustomer = customerRepository.save(customer);
        return customerMapper.toCustomerResponse(updatedCustomer);
    }

    @Transactional
    public CustomerResponse updateCustomerAddress(Long customerId, Map<String, String> updates) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        Address address = customer.getAddress();
        if (address == null) {
            if (updates == null || updates.isEmpty()) {
                return customerMapper.toCustomerResponse(customer);
            }
            address = new Address();
            customer.setAddress(address);
        }

        final Address finalAddress = address;

        updates.forEach((key, value) -> {
            if (value != null && !value.trim().isEmpty()) {
                switch (key) {
                    case "details":
                        finalAddress.setDetails(value);
                        break;
                    case "province":
                        finalAddress.setProvince(value);
                        break;
                    case "district":
                        finalAddress.setDistrict(value);
                        break;
                    default:
                        // Bilinmeyen anahtarları yoksay
                        break;
                }
            }
        });

        Customer updatedCustomer = customerRepository.save(customer);
        return customerMapper.toCustomerResponse(updatedCustomer);
    }

    @Transactional
    public CustomerResponse createTaxInfoForCustomer(Long customerId, TaxInfoRequest taxInfoRequest) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        if (customer.getTaxInfo() != null) {
            throw new IllegalStateException("Customer already has tax info.");
        }

        if (taxInfoRepository.existsByTaxNumber(taxInfoRequest.getTaxNumber())) {
            throw new IllegalStateException("Tax number '" + taxInfoRequest.getTaxNumber() + "' is already in use.");
        }

        TaxInfo taxInfo = taxInfoMapper.toTaxInfo(taxInfoRequest);
        taxInfo.setCustomer(customer);
        customer.setTaxInfo(taxInfo);

        Customer updatedCustomer = customerRepository.save(customer);
        return customerMapper.toCustomerResponse(updatedCustomer);
    }

    // src/main/java/com/firinyonetim/backend/service/CustomerService.java

    @Transactional
    public CustomerResponse createAddressForCustomer(Long customerId, AddressRequest addressRequest) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        if (customer.getAddress() != null) {
            throw new IllegalStateException("Customer already has an address.");
        }

        Address address = addressMapper.toAddress(addressRequest);
        customer.setAddress(address);

        Customer updatedCustomer = customerRepository.save(customer);
        return customerMapper.toCustomerResponse(updatedCustomer);
    }

    @Transactional
    public void assignRoutesToCustomer(Long customerId, List<Long> routeIds) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));
        for (Long routeId : routeIds) {
            Route route = routeRepository.findById(routeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + routeId));
            // Check if assignment already exists to avoid duplicates
            boolean exists = routeAssignmentRepository.findByCustomerId(customerId).stream()
                    .anyMatch(ra -> ra.getRoute().getId().equals(routeId));
            if (!exists) {
                RouteAssignment assignment = new RouteAssignment();
                assignment.setCustomer(customer);
                assignment.setRoute(route);
                routeAssignmentRepository.save(assignment);
            }
        }
    }

    @Transactional
    public CustomerResponse updateCustomerWorkdays(Long customerId, List<String> workdays) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));
        Set<com.firinyonetim.backend.entity.DayOfWeek> days = new HashSet<>();
        if (workdays != null) {
            for (String day : workdays) {
                try {
                    days.add(com.firinyonetim.backend.entity.DayOfWeek.valueOf(day.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid day: " + day);
                }
            }
        }
        customer.setWorkingDays(days);
        Customer updatedCustomer = customerRepository.save(customer);
        return customerMapper.toCustomerResponse(updatedCustomer);
    }
}
