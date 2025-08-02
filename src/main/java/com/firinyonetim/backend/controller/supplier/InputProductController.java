package com.firinyonetim.backend.controller.supplier;
import com.firinyonetim.backend.dto.supplier.request.InputProductRequest;
import com.firinyonetim.backend.dto.supplier.response.InputProductResponse;
import com.firinyonetim.backend.service.supplier.InputProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/input-products")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('YONETICI', 'DEVELOPER', 'MUHASEBE')")
public class InputProductController {
    private final InputProductService inputProductService;

    @GetMapping
    public ResponseEntity<List<InputProductResponse>> getAllInputProducts() {
        return ResponseEntity.ok(inputProductService.getAllInputProducts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<InputProductResponse> getInputProductById(@PathVariable Long id) {
        return ResponseEntity.ok(inputProductService.getInputProductById(id));
    }

    @PostMapping
    public ResponseEntity<InputProductResponse> createInputProduct(@Valid @RequestBody InputProductRequest request) {
        return new ResponseEntity<>(inputProductService.createInputProduct(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<InputProductResponse> updateInputProduct(@PathVariable Long id, @Valid @RequestBody InputProductRequest request) {
        return ResponseEntity.ok(inputProductService.updateInputProduct(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInputProduct(@PathVariable Long id) {
        inputProductService.deleteInputProduct(id);
        return ResponseEntity.noContent().build();
    }
}