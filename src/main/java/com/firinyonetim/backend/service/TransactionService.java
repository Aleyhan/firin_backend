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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final SpecialProductPriceRepository specialPriceRepository;
    private final TransactionMapper transactionMapper;
    private final RouteRepository routeRepository;
    private final TransactionItemRepository transactionItemRepository;



    @Transactional
    public TransactionResponse createTransaction(TransactionCreateRequest request) {
        // 1. Gerekli Varlıkları Bul
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + request.getCustomerId()));
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // 2. Ana Transaction Nesnesini Oluştur
        Transaction transaction = new Transaction();
        transaction.setCustomer(customer);
        transaction.setCreatedBy(currentUser);
        transaction.setNotes(request.getNotes());

        // YENİ: Route (Liste) bilgisini işle
        if (request.getRouteId() != null) {
            Route route = routeRepository.findById(request.getRouteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + request.getRouteId()));
            transaction.setRoute(route);
        }

        BigDecimal balanceChange = BigDecimal.ZERO;

        // 3. İşlem Kalemlerini (Satış/İade) İşle
        if (request.getItems() != null) {
            for (var itemRequest : request.getItems()) {
                Product product = productRepository.findById(itemRequest.getProductId())
                        .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + itemRequest.getProductId()));

                BigDecimal unitPrice = getApplicablePrice(customer.getId(), product.getId());
                BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

                TransactionItem item = new TransactionItem();
                item.setProduct(product);
                item.setQuantity(itemRequest.getQuantity());
                item.setType(itemRequest.getType());
                item.setUnitPrice(unitPrice);
                item.setTotalPrice(totalPrice);
                item.setTransaction(transaction); // Çift yönlü ilişkiyi kur
                transaction.getItems().add(item);

                // Bakiye değişikliğini hesapla
                if (itemRequest.getType() == ItemType.SATIS) {
                    balanceChange = balanceChange.add(totalPrice); // Satış borcu artırır
                } else if (itemRequest.getType() == ItemType.IADE) {
                    balanceChange = balanceChange.subtract(totalPrice); // İade borcu azaltır
                }
            }
        }

        // 4. Tahsilatları İşle
        if (request.getPayments() != null) {
            for (var paymentRequest : request.getPayments()) {
                TransactionPayment payment = new TransactionPayment();
                payment.setAmount(paymentRequest.getAmount());
                payment.setType(paymentRequest.getType());
                payment.setTransaction(transaction); // Çift yönlü ilişkiyi kur
                transaction.getPayments().add(payment);

                // Tahsilat borcu azaltır
                balanceChange = balanceChange.subtract(paymentRequest.getAmount());
            }
        }

        // 5. Müşteri Bakiyesini Güncelle
        customer.setCurrentBalanceAmount(customer.getCurrentBalanceAmount().add(balanceChange));
        customerRepository.save(customer);

        // 6. Transaction'ı Kaydet (Cascade sayesinde item ve payment'lar da kaydedilir)
        Transaction savedTransaction = transactionRepository.save(transaction);

        return transactionMapper.toTransactionResponse(savedTransaction);
    }

    private BigDecimal getApplicablePrice(Long customerId, Long productId) {
        Optional<SpecialProductPrice> specialPriceOpt = specialPriceRepository
                .findByCustomerIdAndProductId(customerId, productId);

        if (specialPriceOpt.isPresent()) {
            return specialPriceOpt.get().getPrice();
        } else {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
            return product.getBasePrice();
        }
    }

    public List<TransactionResponse> getTransactionsByCustomerId(Long customerId) {
        // Müşterinin var olup olmadığını kontrol et (isteğe bağlı ama iyi bir pratik)
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer not found with id: " + customerId);
        }

        // Repository'den müşterinin tüm işlemlerini çek
        List<Transaction> transactions = transactionRepository.findByCustomerIdOrderByTransactionDateDesc(customerId);

        // Çekilen entity listesini, DTO listesine çevir ve döndür
        return transactions.stream()
                .map(transactionMapper::toTransactionResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TransactionResponse updateTransaction(Long transactionId, TransactionUpdateRequest request) {
        // 1. Mevcut Transaction'ı bul
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));

        // 2. Müşteri bakiyesini geri al (eski işlemi iptal et)
        Customer customer = transaction.getCustomer();
        BigDecimal originalBalanceChange = calculateBalanceChange(transaction);
        customer.setCurrentBalanceAmount(customer.getCurrentBalanceAmount().subtract(originalBalanceChange));

        // 3. Transaction'ın temel bilgilerini ve kalemlerini güncelle
        transaction.setNotes(request.getNotes());

        // "Eskiyi sil, yeniyi ekle" stratejisi en güvenlisidir.
        transaction.getItems().clear();
        transaction.getPayments().clear();

        BigDecimal newBalanceChange = BigDecimal.ZERO;

        // 4. Yeni işlem kalemlerini ekle
        if (request.getItems() != null) {
            for (var itemRequest : request.getItems()) {
                // ... createTransaction'daki item ekleme mantığının aynısı ...
                // ... yeni balanceChange'i hesapla ...
            }
        }

        // 5. Yeni ödemeleri ekle
        if (request.getPayments() != null) {
            for (var paymentRequest : request.getPayments()) {
                // ... createTransaction'daki payment ekleme mantığının aynısı ...
                // ... yeni balanceChange'i hesapla ...
            }
        }

        // 6. Yeni bakiye değişikliğini uygula
        customer.setCurrentBalanceAmount(customer.getCurrentBalanceAmount().add(newBalanceChange));

        // 7. Değişiklikleri kaydet
        customerRepository.save(customer);
        Transaction updatedTransaction = transactionRepository.save(transaction);

        return transactionMapper.toTransactionResponse(updatedTransaction);
    }

    @Transactional
    public void deleteTransaction(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));

        // Müşteri bakiyesini geri al
        Customer customer = transaction.getCustomer();
        BigDecimal balanceChange = calculateBalanceChange(transaction);
        customer.setCurrentBalanceAmount(customer.getCurrentBalanceAmount().subtract(balanceChange));
        customerRepository.save(customer);

        // Transaction'ı sil (Cascade sayesinde item ve payment'lar da silinir)
        transactionRepository.delete(transaction);
    }

    // Bakiye değişikliğini hesaplamak için yardımcı metot
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
        // Sorguyu N+1 problemine karşı optimize etmek için EntityGraph kullanabiliriz
        // veya repository'de özel bir @Query yazabiliriz. Şimdilik basit başlayalım.
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));

        return transactionMapper.toTransactionResponse(transaction);
    }

    // ... TransactionService sınıfının içinde ...

    @Transactional
    public TransactionResponse updateTransactionItemPrice(Long transactionId, Long itemId, TransactionItemPriceUpdateRequest request) {
        // 1. İlgili TransactionItem'ı (kalemi) bul.
        // Transaction'a ait olduğundan emin olarak bulmak daha güvenlidir.
        TransactionItem item = transactionItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction item not found with id: " + itemId));

        // Güvenlik kontrolü: Bu item, gerçekten belirtilen transaction'a mı ait?
        if (!item.getTransaction().getId().equals(transactionId)) {
            throw new IllegalStateException("Item with id " + itemId + " does not belong to transaction with id " + transactionId);
        }

        // 2. Eski finansal etkiyi hesapla.
        // Bu kalemin bakiye üzerindeki eski etkisi nedir?
        BigDecimal oldItemBalanceChange = BigDecimal.ZERO;
        if (item.getType() == ItemType.SATIS) {
            oldItemBalanceChange = item.getTotalPrice(); // Satış ise borcu artırmıştı
        } else if (item.getType() == ItemType.IADE) {
            oldItemBalanceChange = item.getTotalPrice().negate(); // İade ise borcu azaltmıştı (negatif etki)
        }

        // 3. Kalemin fiyatını ve toplam fiyatını güncelle.
        BigDecimal newUnitPrice = request.getNewUnitPrice();
        item.setUnitPrice(newUnitPrice);
        item.setTotalPrice(newUnitPrice.multiply(BigDecimal.valueOf(item.getQuantity())));
        transactionItemRepository.save(item); // Değişikliği kaydet

        // 4. Yeni finansal etkiyi hesapla.
        BigDecimal newItemBalanceChange = BigDecimal.ZERO;
        if (item.getType() == ItemType.SATIS) {
            newItemBalanceChange = item.getTotalPrice();
        } else if (item.getType() == ItemType.IADE) {
            newItemBalanceChange = item.getTotalPrice().negate();
        }

        // 5. Müşteri bakiyesini düzelt.
        // Önce eski etkiyi geri al, sonra yeni etkiyi ekle.
        BigDecimal correction = newItemBalanceChange.subtract(oldItemBalanceChange);
        Customer customer = item.getTransaction().getCustomer();
        customer.setCurrentBalanceAmount(customer.getCurrentBalanceAmount().add(correction));
        customerRepository.save(customer);

        // 6. Güncellenmiş Transaction'ın tamamını döndür.
        // findByIdWithDetails ile tüm detayları taze bir şekilde çekiyoruz.
        Transaction updatedTransaction = transactionRepository.findByIdWithDetails(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found after update with id: " + transactionId));

        return transactionMapper.toTransactionResponse(updatedTransaction);
    }



}