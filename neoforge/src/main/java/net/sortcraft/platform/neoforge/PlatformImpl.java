package net.sortcraft.platform.neoforge;

import net.neoforged.fml.loading.FMLPaths;
import net.sortcraft.platform.PlatformService;

import java.nio.file.Path;

/**
 * NeoForge implementation of platform-specific methods.
 * Discovered via {@link java.util.ServiceLoader}.
 */
public class PlatformImpl implements PlatformService {

    @Override
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public boolean isFabric() {
        return false;
    }

    @Override
    public boolean isNeoForge() {
        return true;
    }
}

