// src/main/java/com/firinyonetim/backend/dto/customer/response/LastPaymentDateResponse.java
package com.firinyonetim.backend.dto.customer.response;

import java.time.LocalDate;

public class LastPaymentDateResponse {
    private boolean exists;
    private LocalDate lastPaymentDate;

    public LastPaymentDateResponse(boolean exists, LocalDate lastPaymentDate) {
        this.exists = exists;
        this.lastPaymentDate = lastPaymentDate;
    }

    public boolean isExists() {
        return exists;
    }

    public LocalDate getLastPaymentDate() {
        return lastPaymentDate;
    }
}