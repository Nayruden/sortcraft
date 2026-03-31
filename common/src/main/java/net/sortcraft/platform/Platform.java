package net.sortcraft.platform;

import java.nio.file.Path;
import java.util.ServiceLoader;

/**
 * Platform-specific methods implemented by each loader module.
 * Uses {@link ServiceLoader} to discover the platform implementation at runtime.
 */
public final class Platform {

    private static final PlatformService INSTANCE = ServiceLoader.load(PlatformService.class)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No PlatformService implementation found"));

    private Platform() {}

    /**
     * Gets the config directory path for the mod.
     * @return Path to the config directory
     */
    public static Path getConfigDir() {
        return INSTANCE.getConfigDir();
    }

    /**
     * Checks if we're running on Fabric.
     * @return true if on Fabric loader
     */
    public static boolean isFabric() {
        return INSTANCE.isFabric();
    }

    /**
     * Checks if we're running on NeoForge.
     * @return true if on NeoForge loader
     */
    public static boolean isNeoForge() {
        return INSTANCE.isNeoForge();
    }
}

