package net.sortcraft.compat;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * Helper class for creating Identifier instances.
 * In MC 1.21.11+, ResourceLocation was renamed to Identifier.
 */
public final class IdentifierHelper {
    private IdentifierHelper() {}

    /**
     * Parse a string into an Identifier.
     * @param value The string to parse (e.g., "minecraft:stone")
     * @return The parsed Identifier
     * @throws net.minecraft.ResourceLocationException if the string is invalid
     */
    public static Identifier parse(String value) {
        return Identifier.parse(value);
    }

    /**
     * Try to parse a string into an Identifier.
     * @param value The string to parse (e.g., "minecraft:stone")
     * @return The parsed Identifier, or null if invalid
     */
    public static Identifier tryParse(String value) {
        return Identifier.tryParse(value);
    }

    /**
     * Create an Identifier from namespace and path.
     * @param namespace The namespace (e.g., "minecraft")
     * @param path The path (e.g., "stone")
     * @return The Identifier
     */
    public static Identifier fromNamespaceAndPath(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }

    /**
     * Create an Identifier with the default "minecraft" namespace.
     * @param path The path (e.g., "stone")
     * @return The Identifier
     */
    public static Identifier withDefaultNamespace(String path) {
        return Identifier.withDefaultNamespace(path);
    }

    /**
     * Get the identifier from a ResourceKey as a string.
     * In 1.21.11+, ResourceKey.location() was renamed to ResourceKey.identifier().
     *
     * @param key The ResourceKey
     * @return The identifier as a string
     */
    public static <T> String keyToString(ResourceKey<T> key) {
        return key.identifier().toString();
    }
}

