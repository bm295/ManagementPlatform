package com.managementplatform.domain.enums;

public enum PaymentStatus {
    FAILED("Failed"),
    SUCCEEDED("Succeeded");

    private final String apiName;

    PaymentStatus(String apiName) {
        this.apiName = apiName;
    }

    public String apiName() {
        return apiName;
    }
}
