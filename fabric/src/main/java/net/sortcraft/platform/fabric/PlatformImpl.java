package net.sortcraft.platform.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.sortcraft.platform.PlatformService;

import java.nio.file.Path;

/**
 * Fabric implementation of platform-specific methods.
 * Discovered via {@link java.util.ServiceLoader}.
 */
public class PlatformImpl implements PlatformService {

    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public boolean isFabric() {
        return true;
    }

    @Override
    public boolean isNeoForge() {
        return false;
    }
}

