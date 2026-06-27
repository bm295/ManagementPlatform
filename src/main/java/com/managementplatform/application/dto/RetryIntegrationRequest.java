package com.managementplatform.application.dto;

/**
 * Request DTO for retrying an integration follow-up command.
 *
 * @param idempotencyKey caller-provided key used to make retry commands idempotent
 */
public record RetryIntegrationRequest(
    String idempotencyKey
) {
}
