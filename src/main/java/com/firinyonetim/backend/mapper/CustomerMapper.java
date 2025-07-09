package com.firinyonetim.backend.mapper;

import com.firinyonetim.backend.dto.customer.request.CustomerCreateRequest;
import com.firinyonetim.backend.dto.customer.response.CustomerResponse;
import com.firinyonetim.backend.entity.Customer;
import org.mapstruct.Mapper;

// 'uses' attribute'ü sayesinde, Customer'ı map'lerken
// içindeki adres listesini map'lemek için AddressMapper'ı kullanacağını biliyor.
@Mapper(componentModel = "spring", uses = {AddressMapper.class, SpecialPriceMapper.class})
public interface CustomerMapper {

    // CustomerCreateRequest'ten Customer'a dönüşüm.
    // İçindeki List<AddressRequest> listesini, AddressMapper'ı kullanarak
    // otomatik olarak List<Address> listesine çevirecek.
    Customer toCustomer(CustomerCreateRequest request);

    // Customer'dan CustomerResponse'a dönüşüm.
    // İçindeki List<Address> listesini, AddressMapper'ı kullanarak
    // otomatik olarak List<AddressResponse> listesine çevirecek.
    CustomerResponse toCustomerResponse(Customer customer);
}