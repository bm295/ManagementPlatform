package com.managementplatform.infrastructure.repository;

import com.managementplatform.application.port.out.DeadLetterRepository;
import com.managementplatform.domain.model.DeadLetterMessage;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe in-memory dead-letter repository backed by a {@link ConcurrentHashMap}.
 */
public final class InMemoryDeadLetterRepository implements DeadLetterRepository {
    private final ConcurrentMap<Long, DeadLetterMessage> messagesById = new ConcurrentHashMap<>();

    @Override
    public List<DeadLetterMessage> findRecent() {
        return messagesById.values()
            .stream()
            .sorted(Comparator.comparing(DeadLetterMessage::failedAt).reversed().thenComparingLong(DeadLetterMessage::id))
            .toList();
    }

    @Override
    public void save(DeadLetterMessage message) {
        messagesById.put(message.id(), message);
    }
}
