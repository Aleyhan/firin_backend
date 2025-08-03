// src/main/java/com/firinyonetim/backend/service/supplier/PurchaseService.java
package com.firinyonetim.backend.service.supplier;

import com.firinyonetim.backend.dto.supplier.request.PurchasePaymentRequest;
import com.firinyonetim.backend.dto.supplier.response.PurchasePaymentResponse;
import com.firinyonetim.backend.dto.supplier.request.PurchaseRequest;
import com.firinyonetim.backend.dto.supplier.response.PurchaseResponse;
import com.firinyonetim.backend.entity.User;
import com.firinyonetim.backend.entity.supplier.*;
import com.firinyonetim.backend.exception.ResourceNotFoundException;
import com.firinyonetim.backend.mapper.supplier.PurchaseMapper;
import com.firinyonetim.backend.mapper.supplier.PurchasePaymentMapper;
import com.firinyonetim.backend.repository.supplier.InputProductRepository;
import com.firinyonetim.backend.repository.supplier.PurchasePaymentRepository;
import com.firinyonetim.backend.repository.supplier.PurchaseRepository;
import com.firinyonetim.backend.repository.supplier.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final PurchasePaymentRepository purchasePaymentRepository;
    private final SupplierRepository supplierRepository;
    private final InputProductRepository inputProductRepository;
    private final PurchaseMapper purchaseMapper;
    private final PurchasePaymentMapper purchasePaymentMapper;

    // --- PURCHASE METHODS ---

    @Transactional
    public PurchaseResponse createPurchase(PurchaseRequest request) {
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + request.getSupplierId()));
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Purchase purchase = new Purchase();
        purchase.setSupplier(supplier);
        purchase.setCreatedBy(currentUser);
        purchase.setInvoiceNumber(request.getInvoiceNumber());
        purchase.setNotes(request.getNotes());
        purchase.setPurchaseDate(request.getPurchaseDate() != null ? request.getPurchaseDate() : LocalDateTime.now());

        BigDecimal totalAmount = calculateAndSetItems(purchase, request);
        purchase.setTotalAmount(totalAmount);

        supplier.setCurrentBalanceAmount(supplier.getCurrentBalanceAmount().add(totalAmount));

        Purchase savedPurchase = purchaseRepository.save(purchase);
        return purchaseMapper.toPurchaseResponse(savedPurchase);
    }

    @Transactional
    public PurchaseResponse updatePurchase(Long id, PurchaseRequest request) {
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase not found with id: " + id));
        Supplier supplier = purchase.getSupplier();

        // 1. Eski tutarı bakiyeden düş
        supplier.setCurrentBalanceAmount(supplier.getCurrentBalanceAmount().subtract(purchase.getTotalAmount()));

        // 2. Fatura bilgilerini güncelle
        purchase.setInvoiceNumber(request.getInvoiceNumber());
        purchase.setNotes(request.getNotes());
        purchase.setPurchaseDate(request.getPurchaseDate() != null ? request.getPurchaseDate() : purchase.getPurchaseDate());
        purchase.getItems().clear(); // Önceki ürünleri temizle

        // 3. Yeni ürünleri ve yeni toplam tutarı hesapla
        BigDecimal newTotalAmount = calculateAndSetItems(purchase, request);
        purchase.setTotalAmount(newTotalAmount);

        // 4. Yeni tutarı bakiyeye ekle
        supplier.setCurrentBalanceAmount(supplier.getCurrentBalanceAmount().add(newTotalAmount));

        Purchase updatedPurchase = purchaseRepository.save(purchase);
        return purchaseMapper.toPurchaseResponse(updatedPurchase);
    }

    @Transactional
    public void deletePurchase(Long id) {
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase not found with id: " + id));
        Supplier supplier = purchase.getSupplier();

        // Bakiyeyi düzelt
        supplier.setCurrentBalanceAmount(supplier.getCurrentBalanceAmount().subtract(purchase.getTotalAmount()));

        purchaseRepository.delete(purchase);
    }

    @Transactional(readOnly = true)
    public List<PurchaseResponse> getAllPurchases() {
        return purchaseRepository.findAll().stream()
                .map(purchaseMapper::toPurchaseResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PurchaseResponse getPurchaseById(Long id) {
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase not found with id: " + id));
        return purchaseMapper.toPurchaseResponse(purchase);
    }


    // --- PAYMENT METHODS ---

    @Transactional
    public PurchasePaymentResponse createPayment(PurchasePaymentRequest request) {
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + request.getSupplierId()));
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        PurchasePayment payment = new PurchasePayment();
        payment.setSupplier(supplier);
        payment.setCreatedBy(currentUser);
        payment.setAmount(request.getAmount());
        payment.setType(request.getType());
        payment.setNotes(request.getNotes());
        payment.setPaymentDate(request.getPaymentDate() != null ? request.getPaymentDate() : LocalDateTime.now());

        supplier.setCurrentBalanceAmount(supplier.getCurrentBalanceAmount().subtract(request.getAmount()));

        PurchasePayment savedPayment = purchasePaymentRepository.save(payment);
        return purchasePaymentMapper.toPurchasePaymentResponse(savedPayment);
    }

    @Transactional
    public PurchasePaymentResponse updatePurchasePayment(Long id, PurchasePaymentRequest request) {
        PurchasePayment payment = purchasePaymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
        Supplier supplier = payment.getSupplier();

        // 1. Eski ödeme tutarını bakiyeye geri ekle
        supplier.setCurrentBalanceAmount(supplier.getCurrentBalanceAmount().add(payment.getAmount()));

        // 2. Ödeme bilgilerini güncelle
        payment.setAmount(request.getAmount());
        payment.setType(request.getType());
        payment.setNotes(request.getNotes());
        payment.setPaymentDate(request.getPaymentDate() != null ? request.getPaymentDate() : payment.getPaymentDate());

        // 3. Yeni ödeme tutarını bakiyeden düş
        supplier.setCurrentBalanceAmount(supplier.getCurrentBalanceAmount().subtract(request.getAmount()));

        PurchasePayment updatedPayment = purchasePaymentRepository.save(payment);
        return purchasePaymentMapper.toPurchasePaymentResponse(updatedPayment);
    }

    @Transactional
    public void deletePurchasePayment(Long id) {
        PurchasePayment payment = purchasePaymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
        Supplier supplier = payment.getSupplier();

        // Bakiyeyi düzelt (silinen ödemeyi geri ekle)
        supplier.setCurrentBalanceAmount(supplier.getCurrentBalanceAmount().add(payment.getAmount()));

        purchasePaymentRepository.delete(payment);
    }

    @Transactional(readOnly = true)
    public List<PurchasePaymentResponse> getAllPayments() {
        return purchasePaymentRepository.findAll().stream()
                .map(purchasePaymentMapper::toPurchasePaymentResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PurchasePaymentResponse getPaymentById(Long id) {
        PurchasePayment payment = purchasePaymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
        return purchasePaymentMapper.toPurchasePaymentResponse(payment);
    }

    // --- HELPER METHOD ---
    private BigDecimal calculateAndSetItems(Purchase purchase, PurchaseRequest request) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (var itemRequest : request.getItems()) {
            InputProduct product = inputProductRepository.findById(itemRequest.getInputProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("InputProduct not found with id: " + itemRequest.getInputProductId()));

            PurchaseItem item = new PurchaseItem();
            item.setPurchase(purchase);
            item.setInputProduct(product);
            item.setQuantity(itemRequest.getQuantity());
            item.setUnitPrice(itemRequest.getUnitPrice());
            BigDecimal itemTotal = itemRequest.getQuantity().multiply(itemRequest.getUnitPrice());
            item.setTotalPrice(itemTotal);
            purchase.getItems().add(item);

            totalAmount = totalAmount.add(itemTotal);
        }
        return totalAmount;
    }
}