package com.firinyonetim.backend.mapper;

import com.firinyonetim.backend.dto.address.request.AddressRequest;
import com.firinyonetim.backend.dto.address.response.AddressResponse;
import com.firinyonetim.backend.entity.Address;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AddressMapper {

    // AddressRequest DTO'sundan Address Entity'sine dönüşüm.
    // Bu DTO'da customer bilgisi yok, çünkü bu bilgi CustomerService'de set edilecek.
    @Mapping(target = "zipcode", source = "zipcode")

    Address toAddress(AddressRequest request);

    // Address Entity'sinden AddressResponse DTO'suna dönüşüm.
    // Kaynak (Address) ve hedef (AddressResponse) alanları aynı olduğu için
    // özel bir @Mapping'e gerek yok. MapStruct bunu otomatik yapar.
    @Mapping(target = "zipcode", source = "zipcode")

    AddressResponse toAddressResponse(Address address);
}