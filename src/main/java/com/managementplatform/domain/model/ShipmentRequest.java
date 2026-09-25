package com.managementplatform.domain.model;

import java.time.Instant;

public record ShipmentRequest(long id, long orderId, String warehouse, String productSku, int quantity, Instant createdAt) {
}
