package com.managementplatform;

import com.managementplatform.bootstrap.ManagementPlatformBootstrap;
import java.io.IOException;

/**
 * Backwards-compatible application entry point for the Management Platform demo API.
 */
public final class ManagementPlatformApplication {
    private ManagementPlatformApplication() {
    }

    public static void main(String[] args) throws IOException {
        ManagementPlatformBootstrap.start(System.getenv());
    }
}
