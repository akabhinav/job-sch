package com.enterprise.scheduler.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated response wrapper
 * Feature #43: REST API Design
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Paginated response wrapper")
public class PageResponse<T> {

    @Schema(description = "Page content")
    private List<T> content;

    @Schema(description = "Page number (0-indexed)", example = "0")
    private int page;

    @Schema(description = "Page size", example = "20")
    private int size;

    @Schema(description = "Total elements", example = "100")
    private long totalElements;

    @Schema(description = "Total pages", example = "5")
    private int totalPages;

    @Schema(description = "Is first page")
    private boolean first;

    @Schema(description = "Is last page")
    private boolean last;
}
