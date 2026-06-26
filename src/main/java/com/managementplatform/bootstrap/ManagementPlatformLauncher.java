package com.managementplatform.bootstrap;

import java.io.IOException;

public final class ManagementPlatformLauncher {
    private ManagementPlatformLauncher() {
    }

    public static void main(String[] args) throws IOException {
        ManagementPlatformBootstrap.start(System.getenv());
    }
}
