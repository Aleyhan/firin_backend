package com.firinyonetim.backend.service;

import com.firinyonetim.backend.dto.transaction.request.TransactionCreateRequest;
import com.firinyonetim.backend.dto.transaction.request.TransactionItemPriceUpdateRequest;
import com.firinyonetim.backend.dto.transaction.request.TransactionUpdateRequest;
import com.firinyonetim.backend.dto.transaction.response.TransactionResponse;
import com.firinyonetim.backend.entity.*;
import com.firinyonetim.backend.exception.ResourceNotFoundException;
import com.firinyonetim.backend.mapper.TransactionMapper;
import com.firinyonetim.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final TransactionMapper transactionMapper;
    private final RouteRepository routeRepository;
    private final TransactionItemRepository transactionItemRepository;
    private final CustomerProductAssignmentRepository customerProductAssignmentRepository;

    // YÖNETİCİ İÇİN MEVCUT METOT: Bu metot işlemi oluşturur ve anında ONAYLAR.
    @Transactional
    public TransactionResponse createAndApproveTransaction(TransactionCreateRequest request) {
        Transaction transaction = createTransactionInternal(request, true); // Bakiye güncellenecek
        transaction.setStatus(TransactionStatus.APPROVED); // Durumu ONAYLANDI yap
        Transaction savedTransaction = transactionRepository.save(transaction);
        return transactionMapper.toTransactionResponse(savedTransaction);
    }

    // ŞOFÖR İÇİN YENİ METOT: Bu metot işlemi oluşturur ama ONAY BEKLİYOR olarak bırakır.
    @Transactional
    public TransactionResponse createPendingTransaction(TransactionCreateRequest request) {
        Transaction transaction = createTransactionInternal(request, false); // Bakiye GÜNCELLENMEYECEK
        transaction.setStatus(TransactionStatus.PENDING); // Durumu ONAY BEKLİYOR yap
        Transaction savedTransaction = transactionRepository.save(transaction);
        return transactionMapper.toTransactionResponse(savedTransaction);
    }

    // ORTAK İŞLEM OLUŞTURMA MANTIĞI
    private Transaction createTransactionInternal(TransactionCreateRequest request, boolean updateBalance) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + request.getCustomerId()));
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Transaction transaction = new Transaction();
        transaction.setCustomer(customer);
        transaction.setCreatedBy(currentUser);
        transaction.setNotes(request.getNotes());

        if (request.getTransactionDate() != null) {
            transaction.setTransactionDate(request.getTransactionDate().atStartOfDay());
        } else {
            transaction.setTransactionDate(LocalDateTime.now());
        }

        if (request.getRouteId() != null) {
            Route route = routeRepository.findById(request.getRouteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + request.getRouteId()));
            transaction.setRoute(route);
        }

        BigDecimal balanceChange = BigDecimal.ZERO;

        if (request.getItems() != null) {
            for (var itemRequest : request.getItems()) {
                Product product = productRepository.findById(itemRequest.getProductId())
                        .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + itemRequest.getProductId()));

                CustomerProductAssignment assignment = customerProductAssignmentRepository
                        .findByCustomerIdAndProductId(customer.getId(), product.getId())
                        .orElseThrow(() -> new IllegalStateException("Ürün '" + product.getName() + "' bu müşteriye atanmamış."));

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

        if (request.getPayments() != null) {
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

    // YENİ METOT: İşlemi Onaylama
    @Transactional
    public TransactionResponse approveTransaction(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new IllegalStateException("Sadece onay bekleyen işlemler onaylanabilir.");
        }

        transaction.setStatus(TransactionStatus.APPROVED);

        // Bakiye güncellemesini SADECE bu aşamada yap
        BigDecimal balanceChange = calculateBalanceChange(transaction);
        Customer customer = transaction.getCustomer();
        customer.setCurrentBalanceAmount(customer.getCurrentBalanceAmount().add(balanceChange));
        customerRepository.save(customer);

        Transaction savedTransaction = transactionRepository.save(transaction);
        return transactionMapper.toTransactionResponse(savedTransaction);
    }

    // YENİ METOT: İşlemi Reddetme
    @Transactional
    public TransactionResponse rejectTransaction(Long transactionId, String reason) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new IllegalStateException("Sadece onay bekleyen işlemler reddedilebilir.");
        }

        transaction.setStatus(TransactionStatus.REJECTED);
        transaction.setRejectionReason(reason);

        Transaction savedTransaction = transactionRepository.save(transaction);
        return transactionMapper.toTransactionResponse(savedTransaction);
    }

    // YENİ METOT: Onay bekleyen işlemleri listeleme
    @Transactional(readOnly = true)
    public List<TransactionResponse> getPendingTransactions() {
        return transactionRepository.findByStatusOrderByTransactionDateAsc(TransactionStatus.PENDING)
                .stream()
                .map(transactionMapper::toTransactionResponse)
                .collect(Collectors.toList());
    }

    // YENİ METOT: Şoförün kendi bekleyen işlemini güncellemesi
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

        // Bu işlem bakiye etkilemediği için, eski etkiyi geri almaya gerek yok.
        transaction.setNotes(request.getNotes());
        transaction.getItems().clear();
        transaction.getPayments().clear();

        // Yeni item ve payment'ları ekle (bakiye güncellemesi olmadan)
        if (request.getItems() != null) {
            request.getItems().forEach(itemRequest -> {
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
        if (request.getPayments() != null) {
            request.getPayments().forEach(paymentRequest -> {
                TransactionPayment newPayment = new TransactionPayment();
                newPayment.setAmount(paymentRequest.getAmount());
                newPayment.setType(paymentRequest.getType());
                newPayment.setTransaction(transaction);
                transaction.getPayments().add(newPayment);
            });
        }

        Transaction updatedTransaction = transactionRepository.save(transaction);
        return transactionMapper.toTransactionResponse(updatedTransaction);
    }

    // YENİ METOT: Şoförün kendi bekleyen işlemini silmesi
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

    // --- MEVCUT METOTLAR (DEĞİŞİKLİK YOK) ---
    public List<TransactionResponse> getTransactionsByCustomerId(Long customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer not found with id: " + customerId);
        }
        List<Transaction> transactions = transactionRepository.findByCustomerIdOrderByTransactionDateDesc(customerId);
        return transactions.stream()
                .map(transactionMapper::toTransactionResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteTransaction(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));

        // Sadece onaylanmış bir işlem siliniyorsa bakiyeyi geri al
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
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));
        return transactionMapper.toTransactionResponse(transaction);
    }

    @Transactional
    public TransactionResponse updateTransactionItemPrice(Long transactionId, Long itemId, TransactionItemPriceUpdateRequest request) {
        TransactionItem item = transactionItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction item not found with id: " + itemId));
        if (!item.getTransaction().getId().equals(transactionId)) {
            throw new IllegalStateException("Item with id " + itemId + " does not belong to transaction with id " + transactionId);
        }
        // Sadece onaylanmış işlemlerde bakiye düzeltmesi yap
        if (item.getTransaction().getStatus() == TransactionStatus.APPROVED) {
            BigDecimal oldItemBalanceChange = (item.getType() == ItemType.SATIS) ? item.getTotalPrice() : item.getTotalPrice().negate();
            item.setUnitPrice(request.getNewUnitPrice());
            item.setTotalPrice(request.getNewUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            BigDecimal newItemBalanceChange = (item.getType() == ItemType.SATIS) ? item.getTotalPrice() : item.getTotalPrice().negate();
            BigDecimal correction = newItemBalanceChange.subtract(oldItemBalanceChange);
            Customer customer = item.getTransaction().getCustomer();
            customer.setCurrentBalanceAmount(customer.getCurrentBalanceAmount().add(correction));
            customerRepository.save(customer);
        } else {
            // Onay bekleyen işlemde sadece fiyatı güncelle, bakiye etkilenmez
            item.setUnitPrice(request.getNewUnitPrice());
            item.setTotalPrice(request.getNewUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        transactionItemRepository.save(item);
        Transaction updatedTransaction = transactionRepository.findByIdWithDetails(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found after update with id: " + transactionId));
        return transactionMapper.toTransactionResponse(updatedTransaction);
    }

    @Transactional
    public TransactionResponse updateTransaction(Long transactionId, TransactionUpdateRequest request) {
        Transaction transaction = transactionRepository.findByIdWithDetails(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));
        Customer customer = transaction.getCustomer();

        // Sadece onaylanmışsa eski etkiyi geri al
        if (transaction.getStatus() == TransactionStatus.APPROVED) {
            BigDecimal oldBalanceChange = calculateBalanceChange(transaction);
            customer.setCurrentBalanceAmount(customer.getCurrentBalanceAmount().subtract(oldBalanceChange));
        }

        transaction.setNotes(request.getNotes());
        transaction.getItems().clear();
        transaction.getPayments().clear();

        if (request.getItems() != null) {
            request.getItems().forEach(itemRequest -> {
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
        if (request.getPayments() != null) {
            request.getPayments().forEach(paymentRequest -> {
                TransactionPayment newPayment = new TransactionPayment();
                newPayment.setAmount(paymentRequest.getAmount());
                newPayment.setType(paymentRequest.getType());
                newPayment.setTransaction(transaction);
                transaction.getPayments().add(newPayment);
            });
        }

        // Sadece onaylanmışsa yeni etkiyi ekle
        if (transaction.getStatus() == TransactionStatus.APPROVED) {
            BigDecimal newBalanceChange = calculateBalanceChange(transaction);
            customer.setCurrentBalanceAmount(customer.getCurrentBalanceAmount().add(newBalanceChange));
            customerRepository.save(customer);
        }

        Transaction updatedTransaction = transactionRepository.save(transaction);
        return transactionMapper.toTransactionResponse(updatedTransaction);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> searchTransactions(LocalDate startDate, LocalDate endDate, Long customerId, Long routeId) {
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
        List<Transaction> transactions = transactionRepository.findAll(spec);
        return transactions.stream()
                .map(transactionMapper::toTransactionResponse)
                .collect(Collectors.toList());
    }
}