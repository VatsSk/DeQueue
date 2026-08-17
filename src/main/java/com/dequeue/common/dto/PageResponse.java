package com.dequeue.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;
    
    public static <T, U> PageResponse<T> of(List<T> content, org.springframework.data.domain.Page<U> page) {
        return new PageResponse<>(
            content,
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isLast()
        );
    }

    public static <T> PageResponse<T> empty() {
        return new PageResponse<>(java.util.Collections.emptyList(), 0, 0, 0, 0, true);
    }
}
