// src/main/java/com/firinyonetim/backend/dto/PagedResponseDto.java
package com.firinyonetim.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponseDto<T> {
    private List<T> content;
    private int currentPage;
    private long totalItems;
    private int totalPages;

    public PagedResponseDto(Page<T> page) {
        this.content = page.getContent();
        this.currentPage = page.getNumber();
        this.totalItems = page.getTotalElements();
        this.totalPages = page.getTotalPages();
    }
}