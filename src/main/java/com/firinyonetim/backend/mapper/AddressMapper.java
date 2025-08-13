package com.firinyonetim.backend.mapper;

import com.firinyonetim.backend.dto.address.request.AddressRequest;
import com.firinyonetim.backend.dto.address.response.AddressResponse;
import com.firinyonetim.backend.entity.Address;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AddressMapper {

    // Alan isimleri aynı olduğu için özel bir @Mapping'e gerek yok.
    // latitude ve longitude otomatik olarak map edilecek.
    Address toAddress(AddressRequest request);

    AddressResponse toAddressResponse(Address address);
}