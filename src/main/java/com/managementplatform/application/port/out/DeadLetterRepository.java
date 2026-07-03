package com.managementplatform.application.port.out;

import com.managementplatform.domain.model.DeadLetterMessage;
import java.util.List;

public interface DeadLetterRepository {
    void save(DeadLetterMessage message);
    List<DeadLetterMessage> findRecent();
}
