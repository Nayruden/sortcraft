package net.sortcraft.platform.fabric;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

/**
 * Fabric implementation of platform-specific methods.
 * This class is found by Architectury's @ExpectPlatform annotation.
 * 
 * The class must be in the {original.package}.{platform} subpackage
 * for compatibility with Architectury 13.x (MC 1.21.1).
 */
public class PlatformImpl {

    public static Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    public static boolean isFabric() {
        return true;
    }

    public static boolean isNeoForge() {
        return false;
    }
}

