package com.managementplatform.application.port.out;

import com.managementplatform.domain.model.OutboxMessage;
import com.managementplatform.domain.model.RetryIdempotencyRecord;
import java.util.List;
import java.util.Optional;

public interface IntegrationRetryRepository {
    List<OutboxMessage> findRetryable(int page, int pageSize);
    long countRetryable();
    Optional<OutboxMessage> findByOutboxMessageId(long outboxMessageId);
    Optional<RetryIdempotencyRecord> findRetryIdempotencyRecord(String idempotencyKey);
    void save(OutboxMessage message);
    void saveRetryIdempotencyRecord(RetryIdempotencyRecord record);
}
