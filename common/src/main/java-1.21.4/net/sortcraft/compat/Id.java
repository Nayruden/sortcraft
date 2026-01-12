package net.sortcraft.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Version-agnostic identifier wrapper for Minecraft 1.21.4-1.21.10.
 * Wraps ResourceLocation to provide a unified API across MC versions.
 */
public final class Id implements Comparable<Id> {
    private final ResourceLocation inner;

    private Id(ResourceLocation inner) {
        this.inner = Objects.requireNonNull(inner);
    }

    // === Factory Methods ===

    public static Id of(ResourceLocation rl) {
        return new Id(rl);
    }

    public static Id of(String namespace, String path) {
        return new Id(ResourceLocation.fromNamespaceAndPath(namespace, path));
    }

    public static Id parse(String value) {
        return new Id(ResourceLocation.parse(value));
    }

    public static Id tryParse(String value) {
        ResourceLocation rl = ResourceLocation.tryParse(value);
        return rl != null ? new Id(rl) : null;
    }

    public static Id ofItem(Item item) {
        return new Id(BuiltInRegistries.ITEM.getKey(item));
    }

    /**
     * Get all item IDs from the registry.
     */
    public static Set<Id> allItemIds() {
        return BuiltInRegistries.ITEM.keySet().stream()
                .map(Id::of)
                .collect(Collectors.toSet());
    }

    // === Accessors ===

    public String namespace() {
        return inner.getNamespace();
    }

    public String path() {
        return inner.getPath();
    }

    // === Minecraft API Interop ===

    /**
     * Unwrap to the underlying ResourceLocation for Minecraft API calls.
     */
    public ResourceLocation unwrap() {
        return inner;
    }

    // === Standard Methods ===

    @Override
    public String toString() {
        return inner.toString();
    }

    @Override
    public int hashCode() {
        return inner.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Id other)) return false;
        return inner.equals(other.inner);
    }

    @Override
    public int compareTo(Id other) {
        return inner.compareTo(other.inner);
    }
}

