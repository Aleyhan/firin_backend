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
    private final TransactionMapper transactionMapper;
    private final RouteRepository routeRepository;
    private final TransactionItemRepository transactionItemRepository;
    private final CustomerProductAssignmentRepository customerProductAssignmentRepository;




// TransactionService.java içinde...

    @Transactional
    public TransactionResponse createTransaction(TransactionCreateRequest request) {
        // 1. Gerekli Varlıkları Bul (Bu kısım aynı)
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + request.getCustomerId()));
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // 2. Ana Transaction Nesnesini Oluştur (Bu kısım aynı)
        Transaction transaction = new Transaction();
        transaction.setCustomer(customer);
        transaction.setCreatedBy(currentUser);
        transaction.setNotes(request.getNotes());

        if (request.getRouteId() != null) {
            Route route = routeRepository.findById(request.getRouteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + request.getRouteId()));
            transaction.setRoute(route);
        }

        BigDecimal balanceChange = BigDecimal.ZERO;

        // 3. İşlem Kalemlerini (Satış/İade) İşle (<<< DEĞİŞİKLİK: Bu döngünün içi tamamen değişiyor)
        if (request.getItems() != null) {
            for (var itemRequest : request.getItems()) {
                Product product = productRepository.findById(itemRequest.getProductId())
                        .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + itemRequest.getProductId()));

                // 1. Müşteri-Ürün atamasını ve kurallarını al.
                CustomerProductAssignment assignment = customerProductAssignmentRepository
                        .findByCustomerIdAndProductId(customer.getId(), product.getId())
                        .orElseThrow(() -> new IllegalStateException(
                                "Ürün '" + product.getName() + "' bu müşteriye atanmamış. İşlem yapılamaz."
                        ));

                // 2. Uygulanacak ham fiyatı belirle (Özel fiyat veya standart fiyat).
                BigDecimal basePrice;
                if (assignment.getSpecialPrice() != null) {
                    basePrice = assignment.getSpecialPrice(); // Özel fiyat varsa onu kullan.
                } else {
                    basePrice = product.getBasePrice(); // Yoksa ürünün standart fiyatını kullan.
                }

                // 3. KDV kuralına göre nihai birim satış fiyatını hesapla.
                BigDecimal finalUnitPrice;
                if (assignment.getPricingType() == PricingType.VAT_INCLUDED) {
                    // Fiyat zaten KDV dahil, olduğu gibi al.
                    finalUnitPrice = basePrice;
                } else { // VAT_EXCLUSIVE
                    // Fiyata ürünün KDV'sini ekle.
                    if (product.getVatRate() == null || product.getVatRate() < 0) {
                        throw new IllegalStateException("Ürün '" + product.getName() + "' için geçerli bir KDV oranı tanımlanmamış.");
                    }
                    BigDecimal vatRate = BigDecimal.valueOf(product.getVatRate()).divide(new BigDecimal("100"));
                    BigDecimal vatAmount = basePrice.multiply(vatRate);
                    finalUnitPrice = basePrice.add(vatAmount);
                }

                // 4. TransactionItem'ı bu nihai "fotoğrafı çekilmiş" fiyatla oluştur.
                BigDecimal totalPrice = finalUnitPrice.multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

                TransactionItem item = new TransactionItem();
                item.setProduct(product);
                item.setQuantity(itemRequest.getQuantity());
                item.setType(itemRequest.getType());
                item.setUnitPrice(finalUnitPrice); // <<< ÖNEMLİ: Nihai fiyatı kaydet
                item.setTotalPrice(totalPrice);

                item.setTransaction(transaction);
                transaction.getItems().add(item);

                // Bakiye değişikliğini hesapla (Bu kısım aynı)
                if (itemRequest.getType() == ItemType.SATIS) {
                    balanceChange = balanceChange.add(totalPrice);
                } else if (itemRequest.getType() == ItemType.IADE) {
                    balanceChange = balanceChange.subtract(totalPrice);
                }
            }
        }

        // 4. Tahsilatları İşle (Bu kısım aynı)
        if (request.getPayments() != null) {
            for (var paymentRequest : request.getPayments()) {
                TransactionPayment payment = new TransactionPayment();
                payment.setAmount(paymentRequest.getAmount());
                payment.setType(paymentRequest.getType());
                payment.setTransaction(transaction);
                transaction.getPayments().add(payment);
                // Tahsilat bakiyeden düşülür
                balanceChange = balanceChange.subtract(paymentRequest.getAmount());
            }
        }

        // 5. Müşteri Bakiyesini Güncelle (Bu kısım aynı)
        customer.setCurrentBalanceAmount(customer.getCurrentBalanceAmount().add(balanceChange));
        customerRepository.save(customer);

        // 6. Transaction'ı Kaydet (Bu kısım aynı)
        Transaction savedTransaction = transactionRepository.save(transaction);

        return transactionMapper.toTransactionResponse(savedTransaction);
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