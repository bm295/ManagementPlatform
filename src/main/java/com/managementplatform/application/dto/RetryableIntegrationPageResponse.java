package com.managementplatform.application.dto;

import java.util.List;

/**
 * Paginated response for the operations recovery queue of retryable integrations.
 *
 * @param items retryable integration items on the requested page
 * @param page requested one-based page number
 * @param pageSize requested page size
 * @param totalCount total number of retryable integration items available
 */
public record RetryableIntegrationPageResponse(
    List<RetryableIntegrationItemDto> items,
    int page,
    int pageSize,
    long totalCount
) {
}
