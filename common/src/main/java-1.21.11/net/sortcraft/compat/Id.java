package net.sortcraft.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Version-agnostic identifier wrapper for Minecraft 1.21.11+.
 * In 1.21.11, ResourceLocation was renamed to Identifier.
 */
public final class Id implements Comparable<Id> {
    private final Identifier inner;

    private Id(Identifier inner) {
        this.inner = Objects.requireNonNull(inner);
    }

    // === Factory Methods ===

    public static Id of(Identifier id) {
        return new Id(id);
    }

    public static Id of(String namespace, String path) {
        return new Id(Identifier.fromNamespaceAndPath(namespace, path));
    }

    public static Id parse(String value) {
        return new Id(Identifier.parse(value));
    }

    public static Id tryParse(String value) {
        Identifier id = Identifier.tryParse(value);
        return id != null ? new Id(id) : null;
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
     * Unwrap to the underlying Identifier for Minecraft API calls.
     */
    public Identifier unwrap() {
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

