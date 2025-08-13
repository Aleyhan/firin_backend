package com.firinyonetim.backend.service.supplier;

import com.firinyonetim.backend.dto.address.request.AddressRequest;
import com.firinyonetim.backend.dto.supplier.request.SupplierRequest;
import com.firinyonetim.backend.dto.supplier.response.SupplierResponse;
import com.firinyonetim.backend.dto.supplier.request.SupplierTaxInfoRequest;
import com.firinyonetim.backend.entity.Address;
import com.firinyonetim.backend.entity.supplier.Supplier;
import com.firinyonetim.backend.entity.supplier.SupplierTaxInfo;
import com.firinyonetim.backend.exception.ResourceNotFoundException;
import com.firinyonetim.backend.mapper.AddressMapper;
import com.firinyonetim.backend.mapper.supplier.SupplierMapper;
import com.firinyonetim.backend.mapper.supplier.SupplierTaxInfoMapper;
import com.firinyonetim.backend.repository.supplier.SupplierRepository;
import com.firinyonetim.backend.repository.supplier.SupplierTaxInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierTaxInfoRepository supplierTaxInfoRepository;
    private final SupplierMapper supplierMapper;
    private final AddressMapper addressMapper;
    private final SupplierTaxInfoMapper taxInfoMapper;

    // ... (diğer metotlar aynı kalacak) ...
    @Transactional(readOnly = true)
    public List<SupplierResponse> getAllSuppliers() {
        return supplierRepository.findAll().stream()
                .map(supplierMapper::toSupplierResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SupplierResponse getSupplierById(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + id));
        return supplierMapper.toSupplierResponse(supplier);
    }

    @Transactional
    public SupplierResponse createSupplier(SupplierRequest request) {
        Supplier supplier = supplierMapper.toSupplier(request);
        Supplier savedSupplier = supplierRepository.save(supplier);
        return supplierMapper.toSupplierResponse(savedSupplier);
    }

    @Transactional
    public SupplierResponse updateSupplier(Long id, SupplierRequest request) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + id));
        supplierMapper.updateSupplierFromDto(request, supplier);
        Supplier updatedSupplier = supplierRepository.save(supplier);
        return supplierMapper.toSupplierResponse(updatedSupplier);
    }

    @Transactional
    public void deleteSupplier(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + id));
        supplierRepository.delete(supplier);
    }

    @Transactional
    public SupplierResponse createAddressForSupplier(Long supplierId, AddressRequest addressRequest) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + supplierId));
        if (supplier.getAddress() != null) {
            throw new IllegalStateException("Supplier already has an address.");
        }
        Address address = addressMapper.toAddress(addressRequest);
        supplier.setAddress(address);
        return supplierMapper.toSupplierResponse(supplierRepository.save(supplier));
    }

    @Transactional
    public SupplierResponse updateSupplierAddress(Long supplierId, Map<String, String> updates) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + supplierId));
        Address address = supplier.getAddress();
        if (address == null) {
            throw new ResourceNotFoundException("Address not found for this supplier.");
        }
        updates.forEach((key, value) -> {
            switch (key) {
                case "details": address.setDetails(value); break;
                case "province": address.setProvince(value); break;
                case "district": address.setDistrict(value); break;
            }
        });
        return supplierMapper.toSupplierResponse(supplierRepository.save(supplier));
    }

    @Transactional
    public SupplierResponse createTaxInfoForSupplier(Long supplierId, SupplierTaxInfoRequest taxInfoRequest) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + supplierId));
        if (supplier.getTaxInfo() != null) {
            throw new IllegalStateException("Supplier already has tax info.");
        }

        SupplierTaxInfo taxInfo = taxInfoMapper.toSupplierTaxInfo(taxInfoRequest);
        taxInfo.setSupplier(supplier);
        supplier.setTaxInfo(taxInfo);
        return supplierMapper.toSupplierResponse(supplierRepository.save(supplier));
    }

    @Transactional
    public SupplierResponse updateSupplierTaxInfo(Long supplierId, Map<String, String> updates) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + supplierId));
        SupplierTaxInfo taxInfo = supplier.getTaxInfo();
        if (taxInfo == null) {
            throw new ResourceNotFoundException("Tax info not found for this supplier.");
        }
        updates.forEach((key, value) -> {
            switch (key) {
                case "tradeName": taxInfo.setTradeName(value); break;
                case "taxOffice": taxInfo.setTaxOffice(value); break;
                case "taxNumber":
                    if (supplierTaxInfoRepository.existsByTaxNumberAndIdNot(value, taxInfo.getId())) {
                        throw new IllegalStateException("Tax number is already in use.");
                    }
                    taxInfo.setTaxNumber(value);
                    break;
            }
        });
        return supplierMapper.toSupplierResponse(supplierRepository.save(supplier));
    }

    @Transactional
    public SupplierResponse updateSupplierFields(Long supplierId, Map<String, Object> updates) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + supplierId));

        updates.forEach((key, value) -> {
            switch (key) {
                case "name":
                    supplier.setName((String) value);
                    break;
                case "supplierCode":
                    supplier.setSupplierCode((String) value);
                    break;
                case "contactPerson":
                    supplier.setContactPerson((String) value);
                    break;
                case "phone":
                    supplier.setPhone((String) value);
                    break;
                case "email":
                    supplier.setEmail((String) value);
                    break;
                case "notes":
                    supplier.setNotes((String) value);
                    break;
                // DÜZELTME BURADA: isActive case'i eklendi.
                case "isActive":
                    supplier.setActive((Boolean) value);
                    break;
            }
        });

        return supplierMapper.toSupplierResponse(supplierRepository.save(supplier));
    }
}