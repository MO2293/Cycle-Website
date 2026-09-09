package com.cyclehaven.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * A stable, self-describing envelope for paginated results.
 *
 * <p>Spring's own {@code Page} serialises with a large, unstable JSON shape that
 * is tied to internal Spring types — returning it directly couples every API
 * client to the framework version. This exposes only what a client needs to
 * render a pager.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }
}
