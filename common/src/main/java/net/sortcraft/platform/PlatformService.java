package net.sortcraft.platform;

import java.nio.file.Path;

/**
 * Service interface for platform-specific functionality.
 * Implementations are discovered via {@link java.util.ServiceLoader}.
 */
public interface PlatformService {

    /**
     * Gets the config directory path for the mod.
     * @return Path to the config directory
     */
    Path getConfigDir();

    /**
     * Checks if we're running on Fabric.
     * @return true if on Fabric loader
     */
    boolean isFabric();

    /**
     * Checks if we're running on NeoForge.
     * @return true if on NeoForge loader
     */
    boolean isNeoForge();
}

