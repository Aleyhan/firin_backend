// src/main/java/com/firinyonetim/backend/service/TransactionService.java
package com.firinyonetim.backend.service;

import com.firinyonetim.backend.dto.PagedResponseDto;
import com.firinyonetim.backend.dto.driver.response.DriverDailyCustomerSummaryDto;
import com.firinyonetim.backend.dto.driver.response.DriverTodaysTransactionDto;
import com.firinyonetim.backend.dto.driver.response.DriverTodaysTransactionItemDto;
import com.firinyonetim.backend.dto.transaction.request.TransactionCreateRequest;
import com.firinyonetim.backend.dto.transaction.request.TransactionItemPriceUpdateRequest;
import com.firinyonetim.backend.dto.transaction.request.TransactionItemUpdateRequest;
import com.firinyonetim.backend.dto.transaction.request.TransactionPaymentUpdateRequest;
import com.firinyonetim.backend.dto.transaction.request.TransactionUpdateRequest;
import com.firinyonetim.backend.dto.transaction.response.TransactionResponse;
import com.firinyonetim.backend.entity.*;
import com.firinyonetim.backend.exception.ResourceNotFoundException;
import com.firinyonetim.backend.mapper.TransactionMapper;
import com.firinyonetim.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final TransactionMapper transactionMapper;
    private final RouteRepository routeRepository;
    private final ShipmentRepository shipmentRepository; // YENİ REPOSITORY
    private final CustomerProductAssignmentRepository customerProductAssignmentRepository;
    private final RouteAssignmentRepository routeAssignmentRepository;

    @Transactional
    public TransactionResponse createAndApproveTransaction(TransactionCreateRequest request) {
        Transaction transaction = createTransactionInternal(request, true);
        transaction.setStatus(TransactionStatus.APPROVED);
        Transaction savedTransaction = transactionRepository.save(transaction);
        return transactionMapper.toTransactionResponse(savedTransaction);
    }

    @Transactional
    public TransactionResponse createPendingTransaction(TransactionCreateRequest request) {
        Transaction transaction = createTransactionInternal(request, false);
        transaction.setStatus(TransactionStatus.PENDING);
        Transaction savedTransaction = transactionRepository.save(transaction);
        return transactionMapper.toTransactionResponse(savedTransaction);
    }

    private Transaction createTransactionInternal(TransactionCreateRequest request, boolean updateBalance) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + request.getCustomerId()));
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // YENİ KISIM: Gelen shipmentId ile sevkiyatı bul
        Shipment shipment = shipmentRepository.findById(request.getShipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with id: " + request.getShipmentId()));

        // Güvenlik kontrolü: İşlemi yapan şoför, sevkiyatı başlatan şoförle aynı mı?
        if (!shipment.getDriver().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Bu sevkiyata işlem ekleme yetkiniz yok.");
        }

        Transaction transaction = new Transaction();
        transaction.setCustomer(customer);
        transaction.setCreatedBy(currentUser);
        transaction.setNotes(request.getNotes());
        transaction.setShipment(shipment); // Sevkiyatı işleme bağla
        transaction.setRoute(shipment.getRoute()); // Rotayı sevkiyattan al

        if (request.getTransactionDate() != null) {
            transaction.setTransactionDate(request.getTransactionDate().atTime(LocalTime.now()));
        } else {
            transaction.setTransactionDate(LocalDateTime.now());
        }

        BigDecimal balanceChange = BigDecimal.ZERO;

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            Map<Long, CustomerProductAssignment> assignmentsMap = customerProductAssignmentRepository
                    .findByCustomerId(customer.getId()).stream()
                    .collect(Collectors.toMap(cpa -> cpa.getProduct().getId(), cpa -> cpa));

            for (var itemRequest : request.getItems()) {
                Product product = productRepository.findById(itemRequest.getProductId())
                        .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + itemRequest.getProductId()));

                CustomerProductAssignment assignment = assignmentsMap.get(product.getId());
                if (assignment == null) {
                    throw new IllegalStateException("Ürün '" + product.getName() + "' bu müşteriye atanmamış. İşlem yapılamaz.");
                }

                BigDecimal basePrice = (assignment.getSpecialPrice() != null) ? assignment.getSpecialPrice() : product.getBasePrice();
                BigDecimal finalUnitPrice;
                if (assignment.getPricingType() == PricingType.VAT_INCLUDED) {
                    finalUnitPrice = basePrice;
                } else {
                    BigDecimal vatRate = BigDecimal.valueOf(product.getVatRate()).divide(new BigDecimal("100"));
                    finalUnitPrice = basePrice.add(basePrice.multiply(vatRate));
                }

                BigDecimal totalPrice = finalUnitPrice.multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
                TransactionItem item = new TransactionItem();
                item.setProduct(product);
                item.setQuantity(itemRequest.getQuantity());
                item.setType(itemRequest.getType());
                item.setUnitPrice(finalUnitPrice);
                item.setTotalPrice(totalPrice);
                item.setTransaction(transaction);
                transaction.getItems().add(item);

                if (itemRequest.getType() == ItemType.SATIS) {
                    balanceChange = balanceChange.add(totalPrice);
                } else if (itemRequest.getType() == ItemType.IADE) {
                    balanceChange = balanceChange.subtract(totalPrice);
                }
            }
        }

        if (request.getPayments() != null && !request.getPayments().isEmpty()) {
            for (var paymentRequest : request.getPayments()) {
                TransactionPayment payment = new TransactionPayment();
                payment.setAmount(paymentRequest.getAmount());
                payment.setType(paymentRequest.getType());
                payment.setTransaction(transaction);
                transaction.getPayments().add(payment);
                balanceChange = balanceChange.subtract(paymentRequest.getAmount());
            }
        }

        if (updateBalance) {
            customer.setCurrentBalanceAmount(customer.getCurrentBalanceAmount().add(balanceChange));
            customerRepository.save(customer);
        }

        return transaction;
    }

    // ... (servisin geri kalanı aynı)
    @Transactional
    public TransactionResponse approveTransaction(Long transactionId) {
        Transaction transaction = transactionRepository.findByIdWithDetails(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));

        if (transaction.getStatus() == TransactionStatus.APPROVED) {
            throw new IllegalStateException("Bu işlem zaten onaylanmış.");
        }

        transaction.setStatus(TransactionStatus.APPROVED);
        transaction.setRejectionReason(null);

        BigDecimal balanceChange = calculateBalanceChange(transaction);
        Customer customer = transaction.getCustomer();
        customer.setCurrentBalanceAmount(customer.getCurrentBalanceAmount().add(balanceChange));
        customerRepository.save(customer);

        Transaction savedTransaction = transactionRepository.save(transaction);
        return transactionMapper.toTransactionResponse(savedTransaction);
    }

    @Transactional
    public TransactionResponse rejectTransaction(Long transactionId, String reason) {
        Transaction transaction = transactionRepository.findByIdWithDetails(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));

        if (transaction.getStatus() == TransactionStatus.REJECTED) {
            throw new IllegalStateException("Bu işlem zaten reddedilmiş/iptal edilmiş.");
        }

        if (transaction.getStatus() == TransactionStatus.APPROVED) {
            Customer customer = transaction.getCustomer();
            BigDecimal balanceChange = calculateBalanceChange(transaction);
            customer.setCurrentBalanceAmount(customer.getCurrentBalanceAmount().subtract(balanceChange));
            customerRepository.save(customer);
        }

        transaction.setStatus(TransactionStatus.REJECTED);
        transaction.setRejectionReason(reason);

        Transaction savedTransaction = transactionRepository.save(transaction);
        return transactionMapper.toTransactionResponse(savedTransaction);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getPendingTransactions() {
        return transactionRepository.findByStatusOrderByTransactionDateAsc(TransactionStatus.PENDING)
                .stream()
                .map(transactionMapper::toTransactionResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TransactionResponse updatePendingTransaction(Long transactionId, TransactionUpdateRequest request, Long driverId) {
        Transaction transaction = transactionRepository.findByIdWithDetails(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new IllegalStateException("Sadece onay bekleyen işlemler güncellenebilir.");
        }
        if (!transaction.getCreatedBy().getId().equals(driverId)) {
            throw new AccessDeniedException("Bu işlemi güncelleme yetkiniz yok.");
        }

        updateTransactionItemsAndPayments(transaction, request.getItems(), request.getPayments());
        transaction.setNotes(request.getNotes());


        Transaction updatedTransaction = transactionRepository.save(transaction);
        return transactionMapper.toTransactionResponse(updatedTransaction);
    }

    @Transactional
    public void deletePendingTransaction(Long transactionId, Long driverId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new IllegalStateException("Sadece onay bekleyen işlemler silinebilir.");
        }
        if (!transaction.getCreatedBy().getId().equals(driverId)) {
            throw new AccessDeniedException("Bu işlemi silme yetkiniz yok.");
        }

        transactionRepository.delete(transaction);
    }

    public List<TransactionResponse> getTransactionsByCustomerId(Long customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer not found with id: " + customerId);
        }
        List<Transaction> transactions = transactionRepository.findByCustomerIdOrderByTransactionDateAsc(customerId);
        List<TransactionResponse> responseList = populateDailySequenceNumbers(transactions);
        Collections.reverse(responseList);
        return responseList;
    }

    @Transactional
    public void deleteTransaction(Long transactionId) {
        Transaction transaction = transactionRepository.findByIdWithDetails(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));

        if (transaction.getStatus() == TransactionStatus.APPROVED) {
            Customer customer = transaction.getCustomer();
            BigDecimal balanceChange = calculateBalanceChange(transaction);
            customer.setCurrentBalanceAmount(customer.getCurrentBalanceAmount().subtract(balanceChange));
            customerRepository.save(customer);
        }

        transactionRepository.delete(transaction);
    }

    private BigDecimal calculateBalanceChange(Transaction transaction) {
        BigDecimal balanceChange = BigDecimal.ZERO;
        for (TransactionItem item : transaction.getItems()) {
            if (item.getType() == ItemType.SATIS) {
                balanceChange = balanceChange.add(item.getTotalPrice());
            } else if (item.getType() == ItemType.IADE) {
                balanceChange = balanceChange.subtract(item.getTotalPrice());
            }
        }
        for (TransactionPayment payment : transaction.getPayments()) {
            balanceChange = balanceChange.subtract(payment.getAmount());
        }
        return balanceChange;
    }

    public TransactionResponse getTransactionById(Long transactionId) {
        Transaction transaction = transactionRepository.findByIdWithDetails(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));
        return transactionMapper.toTransactionResponse(transaction);
    }

    @Transactional
    public TransactionResponse updateTransactionItemPrice(Long transactionId, Long itemId, TransactionItemPriceUpdateRequest request) {
        Transaction transaction = transactionRepository.findByIdWithDetails(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));
        TransactionItem item = transaction.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Transaction item not found with id: " + itemId));

        if (transaction.getStatus() == TransactionStatus.APPROVED) {
            BigDecimal oldItemBalanceChange = (item.getType() == ItemType.SATIS) ? item.getTotalPrice() : item.getTotalPrice().negate();
            item.setUnitPrice(request.getNewUnitPrice());
            item.setTotalPrice(request.getNewUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            BigDecimal newItemBalanceChange = (item.getType() == ItemType.SATIS) ? item.getTotalPrice() : item.getTotalPrice().negate();
            BigDecimal correction = newItemBalanceChange.subtract(oldItemBalanceChange);
            Customer customer = item.getTransaction().getCustomer();
            customer.setCurrentBalanceAmount(customer.getCurrentBalanceAmount().add(correction));
            customerRepository.save(customer);
        } else {
            item.setUnitPrice(request.getNewUnitPrice());
            item.setTotalPrice(request.getNewUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        Transaction updatedTransaction = transactionRepository.save(transaction);
        return transactionMapper.toTransactionResponse(updatedTransaction);
    }

    @Transactional
    public TransactionResponse updateTransaction(Long transactionId, TransactionUpdateRequest request) {
        Transaction transaction = transactionRepository.findByIdWithDetails(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));
        Customer customer = transaction.getCustomer();

        if (transaction.getStatus() == TransactionStatus.APPROVED) {
            BigDecimal oldBalanceChange = calculateBalanceChange(transaction);
            customer.setCurrentBalanceAmount(customer.getCurrentBalanceAmount().subtract(oldBalanceChange));
        }

        updateTransactionItemsAndPayments(transaction, request.getItems(), request.getPayments());
        transaction.setNotes(request.getNotes());

        if (transaction.getStatus() == TransactionStatus.APPROVED) {
            BigDecimal newBalanceChange = calculateBalanceChange(transaction);
            customer.setCurrentBalanceAmount(customer.getCurrentBalanceAmount().add(newBalanceChange));
            customerRepository.save(customer);
        }

        Transaction updatedTransaction = transactionRepository.save(transaction);
        return transactionMapper.toTransactionResponse(updatedTransaction);
    }

    private void updateTransactionItemsAndPayments(Transaction transaction, List<TransactionItemUpdateRequest> itemRequests, List<TransactionPaymentUpdateRequest> paymentRequests) {
        transaction.getItems().clear();
        if (itemRequests != null) {
            itemRequests.forEach(itemRequest -> {
                Product product = productRepository.findById(itemRequest.getProductId())
                        .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + itemRequest.getProductId()));
                TransactionItem newItem = new TransactionItem();
                newItem.setProduct(product);
                newItem.setQuantity(itemRequest.getQuantity());
                newItem.setType(itemRequest.getType());
                newItem.setUnitPrice(itemRequest.getUnitPrice());
                newItem.setTotalPrice(itemRequest.getUnitPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity())));
                newItem.setTransaction(transaction);
                transaction.getItems().add(newItem);
            });
        }

        transaction.getPayments().clear();
        if (paymentRequests != null) {
            paymentRequests.forEach(paymentRequest -> {
                TransactionPayment newPayment = new TransactionPayment();
                newPayment.setAmount(paymentRequest.getAmount());
                newPayment.setType(paymentRequest.getType());
                newPayment.setTransaction(transaction);
                transaction.getPayments().add(newPayment);
            });
        }
    }

    @Transactional(readOnly = true)
    public PagedResponseDto<TransactionResponse> searchTransactions(LocalDate startDate, LocalDate endDate, Long customerId, Long routeId, TransactionStatus status, Pageable pageable) {
        Specification<Transaction> spec = Specification.where(null);

        if (startDate != null) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.greaterThanOrEqualTo(root.get("transactionDate"), startDate.atStartOfDay()));
        }
        if (endDate != null) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.lessThanOrEqualTo(root.get("transactionDate"), endDate.atTime(23, 59, 59)));
        }
        if (customerId != null) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("customer").get("id"), customerId));
        }
        if (routeId != null) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("route").get("id"), routeId));
        }
        if (status != null) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("status"), status));
        }

        Page<Transaction> transactionPage = transactionRepository.findAll(spec, pageable);
        Page<TransactionResponse> dtoPage = transactionPage.map(transactionMapper::toTransactionResponse);
        return new PagedResponseDto<>(dtoPage);
    }

    private List<TransactionResponse> populateDailySequenceNumbers(List<Transaction> transactions) {
        Map<LocalDate, Integer> dailyCounters = new HashMap<>();
        return transactions.stream()
                .map(transaction -> {
                    TransactionResponse dto = transactionMapper.toTransactionResponse(transaction);
                    LocalDate date = transaction.getTransactionDate().toLocalDate();
                    int sequence = dailyCounters.merge(date, 1, Integer::sum);
                    dto.setDailySequenceNumber(sequence);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DriverDailyCustomerSummaryDto getDriverDailySummaryForCustomer(Long customerId, Long driverId) {
        if (!routeAssignmentRepository.isCustomerAssignedToDriverActiveRoutes(customerId, driverId)) {
            throw new AccessDeniedException("Bu müşterinin bilgilerini görüntüleme yetkiniz yok.");
        }

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        List<Transaction> todaysTransactions = transactionRepository.findTodaysTransactionsByCustomerAndDriver(customerId, driverId, startOfDay, endOfDay);

        List<DriverTodaysTransactionDto> salesTransactions = new ArrayList<>();
        Map<String, Integer> totalReturnsByProduct = new HashMap<>();

        for (Transaction transaction : todaysTransactions) {
            boolean hasSales = transaction.getItems().stream().anyMatch(item -> item.getType() == ItemType.SATIS);

            if (hasSales) {
                DriverTodaysTransactionDto transactionDto = new DriverTodaysTransactionDto();
                transactionDto.setTransactionId(transaction.getId());
                transactionDto.setTransactionTime(transaction.getTransactionDate());

                List<DriverTodaysTransactionItemDto> itemDtos = transaction.getItems().stream()
                        .filter(item -> item.getType() == ItemType.SATIS)
                        .map(item -> {
                            DriverTodaysTransactionItemDto itemDto = new DriverTodaysTransactionItemDto(item.getProduct().getName());
                            itemDto.setTotalSold(item.getQuantity());
                            return itemDto;
                        }).collect(Collectors.toList());

                transactionDto.setItems(itemDtos);
                salesTransactions.add(transactionDto);
            }

            transaction.getItems().stream()
                    .filter(item -> item.getType() == ItemType.IADE)
                    .forEach(item -> {
                        totalReturnsByProduct.merge(item.getProduct().getName(), item.getQuantity(), Integer::sum);
                    });
        }

        DriverDailyCustomerSummaryDto resultDto = new DriverDailyCustomerSummaryDto();
        resultDto.setSalesTransactions(salesTransactions);
        resultDto.setTotalReturnsByProduct(totalReturnsByProduct);
        return resultDto;
    }
}