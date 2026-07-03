package com.managementplatform.application.port.in;

import com.managementplatform.application.dto.RetryableIntegrationPageResponse;

public interface ListRetryableIntegrationsInputPort {
    RetryableIntegrationPageResponse listRetryable(int page, int pageSize);
}
