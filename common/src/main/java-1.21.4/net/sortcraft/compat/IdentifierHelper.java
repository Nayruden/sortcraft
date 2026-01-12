package net.sortcraft.compat;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/**
 * Helper class for creating ResourceLocation/Identifier instances.
 * In MC 1.21.1-1.21.10, this wraps ResourceLocation.
 * In MC 1.21.11+, this wraps Identifier (which is the renamed ResourceLocation).
 */
public final class IdentifierHelper {
    private IdentifierHelper() {}

    /**
     * Parse a string into a ResourceLocation.
     * @param value The string to parse (e.g., "minecraft:stone")
     * @return The parsed ResourceLocation
     * @throws net.minecraft.ResourceLocationException if the string is invalid
     */
    public static ResourceLocation parse(String value) {
        return ResourceLocation.parse(value);
    }

    /**
     * Try to parse a string into a ResourceLocation.
     * @param value The string to parse (e.g., "minecraft:stone")
     * @return The parsed ResourceLocation, or null if invalid
     */
    public static ResourceLocation tryParse(String value) {
        return ResourceLocation.tryParse(value);
    }

    /**
     * Create a ResourceLocation from namespace and path.
     * @param namespace The namespace (e.g., "minecraft")
     * @param path The path (e.g., "stone")
     * @return The ResourceLocation
     */
    public static ResourceLocation fromNamespaceAndPath(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    /**
     * Create a ResourceLocation with the default "minecraft" namespace.
     * @param path The path (e.g., "stone")
     * @return The ResourceLocation
     */
    public static ResourceLocation withDefaultNamespace(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }

    /**
     * Get the location (ResourceLocation) from a ResourceKey as a string.
     * In 1.21.4-1.21.10, ResourceKey has location() method.
     * In 1.21.11+, this may be renamed.
     *
     * @param key The ResourceKey
     * @return The location as a string
     */
    public static <T> String keyToString(ResourceKey<T> key) {
        return key.location().toString();
    }
}

