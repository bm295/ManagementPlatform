package com.managementplatform.domain.enums;

public enum InvoiceStatus {
    PENDING("Pending"),
    SUCCEEDED("Succeeded"),
    FAILED("Failed");

    private final String apiName;

    InvoiceStatus(String apiName) {
        this.apiName = apiName;
    }

    public String apiName() {
        return apiName;
    }
}
