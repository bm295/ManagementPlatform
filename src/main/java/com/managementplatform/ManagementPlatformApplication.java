package com.managementplatform;

import com.managementplatform.bootstrap.ManagementPlatformBootstrap;
import com.managementplatform.application.dto.CheckoutRequest;
import java.util.Map;

/**
 * Java application entry point for the Management Platform demo API.
 */
public final class ManagementPlatformApplication {
    public static final int DEFAULT_PORT = ManagementPlatformBootstrap.DEFAULT_PORT;

    private ManagementPlatformApplication() {
    }

    public static int portFromEnvironment(Map<String, String> environment) {
        return ManagementPlatformBootstrap.portFromEnvironment(environment);
    }

    static CheckoutRequest checkoutRequestFromJson(String json) {
        return com.managementplatform.presentation.http.ManagementPlatformHttpAdapter.checkoutRequestFromJson(json);
    }
}
