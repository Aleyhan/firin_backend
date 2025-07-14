package com.firinyonetim.backend.dto.customer.request;

import com.firinyonetim.backend.dto.address.request.AddressRequest;
import com.firinyonetim.backend.dto.tax_info.request.TaxInfoRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class CustomerCreateRequest {

    @NotBlank(message = "Müşteri kodu boş olamaz.")
    @Size(min = 4, max = 4, message = "Müşteri kodu tam olarak 4 haneli olmalıdır.")
    // @Pattern(regexp = "\\d{4}", message = "Müşteri kodu sadece 4 rakamdan oluşmalıdır.") // Sadece sayı istiyorsanız bu satırı açın
    private String customerCode;

    @NotBlank(message = "Müşteri adı boş olamaz.")
    private String name;

    private String notes; // Yeni alan


    private String phone;
    private String email;

    // Müşteriyle birlikte adreslerini de alıyoruz.
    private AddressRequest address; // YENİ

    private TaxInfoRequest taxInfo;
}