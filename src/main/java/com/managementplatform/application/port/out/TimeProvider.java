package com.managementplatform.application.port.out;

import java.time.Instant;

public interface TimeProvider {
    Instant now();
}
