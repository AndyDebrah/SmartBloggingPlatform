package com.smartblog.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaginationMetadata {


    private int currentPage;

    private int totalPages;


    private int pageSize;


    private long totalElements;


    private boolean hasNext;


    private boolean hasPrevious;


    private boolean isFirst;


    private boolean isLast;

    public static PaginationMetadata from(Page<?> page) {
        return PaginationMetadata.builder()
                .currentPage(page.getNumber())
                .totalPages(page.getTotalPages())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .build();
    }
}
