package com.managementplatform.domain.enums;

public enum OutboxStatus {
    PENDING("Pending"),
    PROCESSING("Processing"),
    SUCCEEDED("Succeeded"),
    FAILED("Failed");

    private final String apiName;

    OutboxStatus(String apiName) {
        this.apiName = apiName;
    }

    public String apiName() {
        return apiName;
    }
}
