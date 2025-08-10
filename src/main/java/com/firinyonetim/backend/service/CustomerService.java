// src/main/java/com/firinyonetim/backend/service/CustomerService.java
package com.firinyonetim.backend.service;

import com.firinyonetim.backend.dto.PagedResponseDto;
import com.firinyonetim.backend.dto.address.request.AddressRequest;
import com.firinyonetim.backend.dto.customer.request.CustomerCreateRequest;
import com.firinyonetim.backend.dto.customer.request.CustomerProductAssignmentRequest;
import com.firinyonetim.backend.dto.customer.request.CustomerUpdateRequest;
import com.firinyonetim.backend.dto.customer.response.CustomerProductAssignmentResponse;
import com.firinyonetim.backend.dto.customer.response.CustomerResponse;
import com.firinyonetim.backend.dto.customer.response.LastPaymentDateResponse;
import com.firinyonetim.backend.dto.product.response.AffectedCustomerDto;
import com.firinyonetim.backend.dto.tax_info.request.TaxInfoRequest;
import com.firinyonetim.backend.dto.transaction.response.TransactionResponse;
import com.firinyonetim.backend.entity.*;
import com.firinyonetim.backend.ewaybill.repository.EWaybillTemplateRepository;
import com.firinyonetim.backend.exception.ResourceNotFoundException;
import com.firinyonetim.backend.mapper.*;
import com.firinyonetim.backend.repository.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;


@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final CustomerMapper customerMapper;
    private final TaxInfoRepository taxInfoRepository;
    private final AddressMapper addressMapper;
    private final TransactionPaymentRepository transactionPaymentRepository;
    private final RouteRepository routeRepository;
    private final RouteAssignmentRepository routeAssignmentRepository;
    private final CustomerProductAssignmentRepository customerProductAssignmentRepository;
    private final CustomerProductAssignmentMapper customerProductAssignmentMapper;
    private final TaxInfoMapper taxInfoMapper;
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final EWaybillTemplateRepository ewaybillTemplateRepository; // YENİ REPOSITORY




    @Transactional(readOnly = true)
    public PagedResponseDto<CustomerResponse> searchCustomers(String searchTerm, Long routeId, Boolean status, Boolean hasSpecialPrice, Pageable pageable) { // YENİ PARAMETRE
        Specification<Customer> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(searchTerm)) {
                String likePattern = "%" + searchTerm.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), likePattern),
                        cb.like(cb.lower(root.get("customerCode")), likePattern)
                ));
            }

            if (routeId != null) {
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<RouteAssignment> subRoot = subquery.from(RouteAssignment.class);
                subquery.select(subRoot.get("customer").get("id"));
                subquery.where(cb.equal(subRoot.get("route").get("id"), routeId));
                predicates.add(root.get("id").in(subquery));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("isActive"), status));
            }

            // YENİ FİLTRELEME MANTIĞI
            if (Boolean.TRUE.equals(hasSpecialPrice)) {
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<CustomerProductAssignment> subRoot = subquery.from(CustomerProductAssignment.class);
                subquery.select(subRoot.get("customer").get("id"));
                subquery.where(cb.isNotNull(subRoot.get("specialPrice")));
                predicates.add(root.get("id").in(subquery));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Customer> customerPage = customerRepository.findAll(spec, pageable);
        List<Customer> customersOnPage = customerPage.getContent();

        if (customersOnPage.isEmpty()) {
            return new PagedResponseDto<>(customerPage.map(customerMapper::toCustomerResponse));
        }

        List<Long> customerIds = customersOnPage.stream().map(Customer::getId).collect(Collectors.toList());

        // YENİ: Şablonu olan müşteri ID'lerini al
        Set<Long> customerIdsWithTemplate = ewaybillTemplateRepository.findByCustomerIdIn(customerIds).stream()
                .map(template -> template.getCustomer().getId())
                .collect(Collectors.toSet());


        List<RouteAssignment> allAssignments = routeAssignmentRepository.findAllWithDetails();
        Map<Long, List<Long>> customerToRouteIdsMap = allAssignments.stream()
                .collect(groupingBy(
                        assignment -> assignment.getCustomer().getId(),
                        mapping(assignment -> assignment.getRoute().getId(), toList())
                ));

        Map<Long, LocalDateTime> lastPaymentDateMap = transactionPaymentRepository.findLastPaymentDatesForCustomerIds(customerIds)
                .stream()
                .collect(Collectors.toMap(
                        map -> (Long) map.get("customerId"),
                        map -> (LocalDateTime) map.get("lastPaymentDate")
                ));

        Page<CustomerResponse> dtoPage = customerPage.map(customer -> {
            CustomerResponse response = customerMapper.toCustomerResponse(customer);
            response.setRouteIds(customerToRouteIdsMap.getOrDefault(customer.getId(), Collections.emptyList()));
            response.setLastPaymentDate(lastPaymentDateMap.get(customer.getId()));
            // YENİ: Alanı doldur
            response.setHasEWaybillTemplate(customerIdsWithTemplate.contains(customer.getId()));

            return response;
        });

        return new PagedResponseDto<>(dtoPage);
    }


    public List<CustomerResponse> getAllCustomers() {
        List<Customer> customers = customerRepository.findAll();
        List<RouteAssignment> allAssignments = routeAssignmentRepository.findAll();

        Set<Long> customerIdsWithTemplate = ewaybillTemplateRepository.findAll().stream()
                .map(template -> template.getCustomer().getId())
                .collect(Collectors.toSet());

        Map<Long, List<Long>> customerToRouteIdsMap = allAssignments.stream()
                .collect(groupingBy(
                        assignment -> assignment.getCustomer().getId(),
                        mapping(assignment -> assignment.getRoute().getId(), toList())
                ));

        return customers.stream()
                .map(customer -> {
                    CustomerResponse response = customerMapper.toCustomerResponse(customer);
                    response.setRouteIds(customerToRouteIdsMap.getOrDefault(customer.getId(), List.of()));
                    // YENİ: Alanı doldur
                    response.setHasEWaybillTemplate(customerIdsWithTemplate.contains(customer.getId()));

                    return response;
                })
                .collect(Collectors.toList());
    }

    // CustomerService.java içinde bu metodu bulun ve değiştirin

    public CustomerResponse getCustomerById(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found with id: " + customerId));
        CustomerResponse response = customerMapper.toCustomerResponse(customer);

        List<Long> routeIds = routeAssignmentRepository.findByCustomerId(customerId)
                .stream()
                .map(ra -> ra.getRoute().getId())
                .collect(Collectors.toList());
        response.setRouteIds(routeIds);

        // YENİ EKLENEN KISIM: İşlem geçmişini (ledger) çekip DTO'ya ekliyoruz.
        List<Transaction> transactions = transactionRepository.findByCustomerIdOrderByTransactionDateAsc(customerId);
        List<TransactionResponse> ledger = transactions.stream()
                .map(transactionMapper::toTransactionResponse)
                .collect(Collectors.toList());
        response.setLedger(ledger);

        return response;
    }

    @Transactional
    public CustomerResponse createCustomer(CustomerCreateRequest request) {

        if (customerRepository.existsByCustomerCode(request.getCustomerCode())) {
            throw new IllegalStateException("Customer code " + request.getCustomerCode() + " is already in use.");
        }
        Customer customer = customerMapper.toCustomer(request);

        if (customer.getTaxInfo() != null) {
            customer.getTaxInfo().setCustomer(customer);
        }

        if (request.getWorkingDays() != null) {
            customer.setWorkingDays(new HashSet<>(request.getWorkingDays()));
        } else {
            customer.setWorkingDays(EnumSet.allOf(com.firinyonetim.backend.entity.DayOfWeek.class));
        }

        if (request.getIrsaliyeGunleri() != null) {
            customer.setIrsaliyeGunleri(new HashSet<>(request.getIrsaliyeGunleri()));
        } else {
            customer.setIrsaliyeGunleri(EnumSet.allOf(com.firinyonetim.backend.entity.DayOfWeek.class));
        }

        Customer savedCustomer = customerRepository.save(customer);

        CustomerResponse response = customerMapper.toCustomerResponse(savedCustomer);

        return response;
    }

    public LastPaymentDateResponse getLastPaymentDate(Long customerId) {
        return transactionPaymentRepository.findLastPaymentDateByCustomerId(customerId)
                .map(dateTime -> new LastPaymentDateResponse(true, dateTime.toLocalDate()))
                .orElse(new LastPaymentDateResponse(false, null));
    }


    @Transactional
    public CustomerResponse updateCustomer(Long customerId, CustomerUpdateRequest request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        customer.setName(request.getName());
        customer.setPhone(request.getPhone());
        customer.setEmail(request.getEmail());
        customer.setActive(request.getIsActive());
        customer.setNotes(request.getNotes());

        if (request.getTaxInfo() != null) {
            TaxInfo taxInfo = customer.getTaxInfo();
            if (taxInfo == null) {
                taxInfo = new TaxInfo();
                taxInfo.setCustomer(customer);
                customer.setTaxInfo(taxInfo);
            }
            taxInfo.setTradeName(request.getTaxInfo().getTradeName());
            taxInfo.setTaxNumber(request.getTaxInfo().getTaxNumber());
            taxInfo.setTaxOffice(request.getTaxInfo().getTaxOffice());
        } else if (customer.getTaxInfo() != null) {
            customer.setTaxInfo(null);
        }

        if (request.getAddress() != null) {
            Address address = customer.getAddress();
            if (address == null) {
                address = new Address();
                customer.setAddress(address);
            }
            address.setDetails(request.getAddress().getDetails());
            address.setProvince(request.getAddress().getProvince());
            address.setDistrict(request.getAddress().getDistrict());
        } else {
            customer.setAddress(null);
        }

        if (request.getWorkingDays() != null) {
            customer.setWorkingDays(new HashSet<>(request.getWorkingDays()));
        }

        Customer updatedCustomer = customerRepository.save(customer);

        return customerMapper.toCustomerResponse(updatedCustomer);
    }

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

                            if (newCode.length() != 4) {
                                throw new IllegalArgumentException("Müşteri kodu tam olarak 4 haneli olmalıdır.");
                            }

                            if (customerRepository.existsByCustomerCodeAndIdNot(newCode, customer.getId())) {
                                throw new IllegalStateException("Müşteri kodu '" + newCode + "' zaten başka bir müşteri tarafından kullanılıyor.");
                            }

                            customer.setCustomerCode(newCode);
                            break;
                    }
                }
        );

        Customer updatedCustomer = customerRepository.save(customer);
        return customerMapper.toCustomerResponse(updatedCustomer);
    }

    @Transactional
    public CustomerResponse updateCustomerTaxInfo(Long customerId, Map<String, String> updates) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        TaxInfo taxInfo = customer.getTaxInfo();
        if (taxInfo == null) {
            if (updates == null || updates.isEmpty()) {
                return customerMapper.toCustomerResponse(customer);
            }
            taxInfo = new TaxInfo();
            taxInfo.setCustomer(customer);
            customer.setTaxInfo(taxInfo);
        }

        final TaxInfo finalTaxInfo = taxInfo;

        updates.forEach((key, value) -> {
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
                    if (taxInfoRepository.existsByTaxNumberAndCustomerIdNot(value, customerId)) {
                        throw new IllegalStateException("Vergi numarası '" + value + "' zaten başka bir müşteri tarafından kullanılıyor.");
                    }
                    finalTaxInfo.setTaxNumber(value);
                    break;
                default:
                    break;
            }
        });

        Customer updatedCustomer = customerRepository.save(customer);
        return customerMapper.toCustomerResponse(updatedCustomer);
    }

    @Transactional
    public CustomerResponse updateCustomerAddress(Long customerId, Map<String, String> updates) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        Address address = customer.getAddress();
        if (address == null) {
            // Eğer adres yoksa ve güncelleme isteği geldiyse, yeni bir adres oluştur.
            if (updates == null || updates.isEmpty()) {
                return customerMapper.toCustomerResponse(customer);
            }
            address = new Address();
            customer.setAddress(address);
        }

        final Address finalAddress = address;

        updates.forEach((key, value) -> {
            // Değerin null olup olmadığını kontrol etmeye devam ediyoruz,
            // ancak boş string ("") gönderilmesine izin veriyoruz ki bir alan temizlenebilsin.
            if (value != null) {
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
                    // --- YENİ CASE EKLENDİ ---
                    case "zipcode":
                        finalAddress.setZipcode(value);
                        break;
                    // --- YENİ CASE SONU ---
                    default:
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

    @Transactional
    public CustomerResponse updateCustomerIrsaliyeGunleri(Long customerId, List<String> irsaliyeGunleri) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));
        Set<com.firinyonetim.backend.entity.DayOfWeek> days = new HashSet<>();
        if (irsaliyeGunleri != null) {
            for (String day : irsaliyeGunleri) {
                try {
                    days.add(com.firinyonetim.backend.entity.DayOfWeek.valueOf(day.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid day: " + day);
                }
            }
        }
        customer.setIrsaliyeGunleri(days);
        Customer updatedCustomer = customerRepository.save(customer);
        return customerMapper.toCustomerResponse(updatedCustomer);
    }

    // BU METODU BUL VE İÇERİĞİNİ GÜNCELLE
    @Transactional
    public CustomerProductAssignmentResponse assignOrUpdateProductToCustomer(Long customerId, CustomerProductAssignmentRequest request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        CustomerProductAssignment assignment = customerProductAssignmentRepository
                .findByCustomerIdAndProductId(customerId, request.getProductId())
                .orElse(new CustomerProductAssignment());

        // Fiyatla ilgili eski değerleri sakla
        PricingType oldPricingType = assignment.getPricingType();
        BigDecimal oldSpecialPrice = assignment.getSpecialPrice();
        boolean isNewAssignment = assignment.getId() == null;

        assignment.setCustomer(customer);
        assignment.setProduct(product);
        assignment.setPricingType(request.getPricingType());
        assignment.setSpecialPrice(request.getSpecialPrice());

        // Fiyatla ilgili alanlar değişti mi diye kontrol et
        boolean priceChanged = !Objects.equals(oldPricingType, request.getPricingType()) || !Objects.equals(oldSpecialPrice, request.getSpecialPrice());

        // Yeni atama ise veya fiyat değiştiyse tarihi güncelle
        if (isNewAssignment || priceChanged) {
            assignment.setPriceUpdatedAt(LocalDateTime.now());
        }

        BigDecimal priceToUse = assignment.getSpecialPrice() != null ? assignment.getSpecialPrice() : product.getBasePrice();
        BigDecimal vatRate = BigDecimal.valueOf(product.getVatRate()).divide(new BigDecimal("100"));
        BigDecimal vatMultiplier = BigDecimal.ONE.add(vatRate);

        if (assignment.getPricingType() == PricingType.VAT_INCLUDED) {
            assignment.setFinalPriceVatIncluded(priceToUse.setScale(4, RoundingMode.HALF_UP));
            assignment.setFinalPriceVatExclusive(priceToUse.divide(vatMultiplier, 4, RoundingMode.HALF_UP));
        } else { // VAT_EXCLUSIVE
            assignment.setFinalPriceVatExclusive(priceToUse.setScale(4, RoundingMode.HALF_UP));
            assignment.setFinalPriceVatIncluded(priceToUse.multiply(vatMultiplier).setScale(4, RoundingMode.HALF_UP));
        }

        CustomerProductAssignment savedAssignment = customerProductAssignmentRepository.save(assignment);
        return customerProductAssignmentMapper.toResponse(savedAssignment);
    }

    // DEĞİŞTİRİLEN METOT
    public List<CustomerProductAssignmentResponse> getCustomerProductAssignments(Long customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer not found with id: " + customerId);
        }

        List<CustomerProductAssignment> assignments = customerProductAssignmentRepository.findByCustomerId(customerId);

        // Artık hesaplama yapmaya gerek yok, mapper doğrudan entity'deki yeni alanları DTO'ya çevirecek.
        return assignments.stream()
                .map(customerProductAssignmentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void removeAssignedProduct(Long customerId, Long productId) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer not found with id: " + customerId);
        }
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found with id: " + productId);
        }
        boolean exists = customerProductAssignmentRepository.findByCustomerIdAndProductId(customerId, productId).isPresent();
        if (!exists) {
            throw new ResourceNotFoundException("Assignment not found for customerId: " + customerId + " and productId: " + productId);
        }
        customerProductAssignmentRepository.deleteByCustomerIdAndProductId(customerId, productId);
    }


}