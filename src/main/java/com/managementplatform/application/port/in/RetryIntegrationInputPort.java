package com.managementplatform.application.port.in;

import com.managementplatform.application.dto.RetryIntegrationRequest;
import com.managementplatform.application.dto.RetryIntegrationResponse;

public interface RetryIntegrationInputPort {
    RetryIntegrationResponse retry(long outboxMessageId, RetryIntegrationRequest request);
}
