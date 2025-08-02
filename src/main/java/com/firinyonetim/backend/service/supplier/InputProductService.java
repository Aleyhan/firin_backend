package com.firinyonetim.backend.service.supplier;

import com.firinyonetim.backend.dto.supplier.request.InputProductRequest;
import com.firinyonetim.backend.dto.supplier.response.InputProductResponse;
import com.firinyonetim.backend.entity.supplier.InputProduct;
import com.firinyonetim.backend.exception.ResourceNotFoundException;
import com.firinyonetim.backend.mapper.supplier.InputProductMapper;
import com.firinyonetim.backend.repository.supplier.InputProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InputProductService {

    private final InputProductRepository inputProductRepository;
    private final InputProductMapper inputProductMapper;

    // ... (diğer metotlar aynı) ...
    @Transactional(readOnly = true)
    public List<InputProductResponse> getAllInputProducts() {
        return inputProductRepository.findAll().stream()
                .map(inputProductMapper::toInputProductResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InputProductResponse getInputProductById(Long id) {
        InputProduct product = inputProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("InputProduct not found with id: " + id));
        return inputProductMapper.toInputProductResponse(product);
    }

    @Transactional
    public InputProductResponse createInputProduct(InputProductRequest request) {
        InputProduct product = inputProductMapper.toInputProduct(request);
        // Yeni ürün oluşturulurken isActive durumu request'ten gelmiyorsa varsayılan olarak true ayarla
        if (request.getIsActive() == null) {
            product.setActive(true);
        }
        InputProduct savedProduct = inputProductRepository.save(product);
        return inputProductMapper.toInputProductResponse(savedProduct);
    }

    @Transactional
    public InputProductResponse updateInputProduct(Long id, InputProductRequest request) {
        InputProduct product = inputProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("InputProduct not found with id: " + id));

        // DÜZELTME: Manuel kontrol kaldırıldı, artık tüm işi Mapper yapıyor.
        inputProductMapper.updateInputProductFromDto(request, product);

        InputProduct updatedProduct = inputProductRepository.save(product);
        return inputProductMapper.toInputProductResponse(updatedProduct);
    }

    @Transactional
    public void deleteInputProduct(Long id) {
        InputProduct product = inputProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("InputProduct not found with id: " + id));
        inputProductRepository.delete(product);
    }
}