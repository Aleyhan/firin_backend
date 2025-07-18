package com.firinyonetim.backend.mapper;

import com.firinyonetim.backend.dto.customer.request.CustomerCreateRequest;
import com.firinyonetim.backend.dto.customer.request.CustomerUpdateRequest;
import com.firinyonetim.backend.dto.customer.response.CustomerResponse;
import com.firinyonetim.backend.entity.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

// 'uses' attribute'ü sayesinde, Customer'ı map'lerken
// içindeki adres listesini map'lemek için AddressMapper'ı kullanacağını biliyor.
@Mapper(componentModel = "spring", uses = {AddressMapper.class, TaxInfoMapper.class})
public interface CustomerMapper {

    // CustomerCreateRequest'ten Customer'a dönüşüm.
    // İçindeki List<AddressRequest> listesini, AddressMapper'ı kullanarak
    // otomatik olarak List<Address> listesine çevirecek.

    // Customer'dan CustomerResponse'a dönüşüm.
    // İçindeki List<Address> listesini, AddressMapper'ı kullanarak
    // otomatik olarak List<AddressResponse> listesine çevirecek.
    @Mapping(source = "workingDays", target = "workingDays")
    @Mapping(source = "irsaliyeGunleri", target = "irsaliyeGunleri")
    CustomerResponse toCustomerResponse(Customer customer);

    @Mapping(source = "irsaliyeGunleri", target = "irsaliyeGunleri")
    Customer toCustomer(CustomerCreateRequest request);

    @Mapping(source = "irsaliyeGunleri", target = "irsaliyeGunleri")
    void updateCustomerFromDto(CustomerUpdateRequest request, @MappingTarget Customer customer);

}