package com.managementplatform.application.dto;

import com.managementplatform.domain.enums.OutboxMessageType;
import com.managementplatform.domain.enums.OutboxStatus;

/**
 * Status of a checkout follow-up integration or dead-lettered payment step.
 */
public record IntegrationStatusDto(
    OutboxMessageType type,
    OutboxStatus status,
    int attemptCount,
    String lastError
) {
}
