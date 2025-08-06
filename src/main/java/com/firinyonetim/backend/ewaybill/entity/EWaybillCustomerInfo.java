package com.firinyonetim.backend.ewaybill.entity;

import com.firinyonetim.backend.entity.Customer;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "e_waybill_customer_info")
public class EWaybillCustomerInfo {

    @Id
    private Long id; // Customer ID'yi Primary Key olarak kullanacağız

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EWaybillRecipientType recipientType;

    // Sadece recipientType = REGISTERED_USER ise doldurulur.
    // Birden fazla alias'ı varsa, varsayılan olarak kullanılacak olan buraya yazılır.
    @Column
    private String defaultAlias;
}