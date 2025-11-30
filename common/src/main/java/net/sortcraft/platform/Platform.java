package net.sortcraft.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;

import java.nio.file.Path;

/**
 * Platform-specific methods implemented by each loader module.
 */
public final class Platform {
    
    /**
     * Gets the config directory path for the mod.
     * @return Path to the config directory
     */
    @ExpectPlatform
    public static Path getConfigDir() {
        throw new AssertionError("Not implemented");
    }
    
    /**
     * Checks if we're running on Fabric.
     * @return true if on Fabric loader
     */
    @ExpectPlatform
    public static boolean isFabric() {
        throw new AssertionError("Not implemented");
    }
    
    /**
     * Checks if we're running on NeoForge.
     * @return true if on NeoForge loader
     */
    @ExpectPlatform
    public static boolean isNeoForge() {
        throw new AssertionError("Not implemented");
    }
}

