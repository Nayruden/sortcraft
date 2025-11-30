package net.sortcraft.platform.neoforge;

import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

/**
 * NeoForge implementation of platform-specific methods.
 * This class is found by Architectury's @ExpectPlatform annotation.
 * 
 * The class must be in the {original.package}.{platform} subpackage
 * for compatibility with Architectury 13.x (MC 1.21.1).
 */
public class PlatformImpl {

    public static Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    public static boolean isFabric() {
        return false;
    }

    public static boolean isNeoForge() {
        return true;
    }
}

