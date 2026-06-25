package com.managementplatform.bootstrap;

import java.io.IOException;
import java.util.Map;

public final class ManagementPlatformLauncher {
    private ManagementPlatformLauncher() {
    }

    public static void main(String[] args) throws IOException {
        ManagementPlatformBootstrap.start(System.getenv());
    }

    public static int portFromEnvironment(Map<String, String> environment) {
        return ManagementPlatformBootstrap.portFromEnvironment(environment);
    }
}
