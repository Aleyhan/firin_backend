// src/main/java/com/firinyonetim/backend/ewaybill/dto/response/BulkSendResultDto.java
package com.firinyonetim.backend.ewaybill.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkSendResultDto {
    private UUID id;
    private String ewaybillNumber; // Başarılıysa numara döner
    private boolean success;
    private String message; // Başarısızsa hata mesajı döner
}