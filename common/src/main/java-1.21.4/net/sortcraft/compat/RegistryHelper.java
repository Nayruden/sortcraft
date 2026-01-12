package net.sortcraft.compat;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Registry helper for Minecraft 1.21.4+
 * In 1.21.4+, BuiltInRegistries.ITEM.get() returns Optional<Holder.Reference<Item>>
 * In 1.21.4+, registries.lookupOrThrow() returns HolderLookup.RegistryLookup<T>
 */
public final class RegistryHelper {
    private RegistryHelper() {}

    /**
     * Get an item from the registry, throwing if not found.
     */
    public static Item getItemOrThrow(Id id) {
        return BuiltInRegistries.ITEM.get(id.unwrap())
            .orElseThrow(() -> new IllegalArgumentException("Unknown item: " + id))
            .value();
    }

    /**
     * Get an item from the registry, returning null if not found.
     */
    public static Item getItemOrNull(Id id) {
        return BuiltInRegistries.ITEM.get(id.unwrap())
            .map(ref -> ref.value())
            .orElse(null);
    }

    /**
     * Get an item by key, returning the default item if not found.
     */
    public static Item getItemByKey(Id id) {
        return BuiltInRegistries.ITEM.getValue(id.unwrap());
    }

    /**
     * Get an enchantment by its Id from the registry access.
     * Returns null if not found.
     */
    public static Enchantment getEnchantmentOrNull(RegistryAccess registries, Id id) {
        HolderLookup.RegistryLookup<Enchantment> enchantmentRegistry = registries.lookupOrThrow(Registries.ENCHANTMENT);
        ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, id.unwrap());
        return enchantmentRegistry.get(key)
            .map(holder -> holder.value())
            .orElse(null);
    }

    /**
     * Get an enchantment by its Id from the registry access.
     * Throws if not found.
     */
    public static Enchantment getEnchantmentOrThrow(RegistryAccess registries, Id id) {
        Enchantment enchantment = getEnchantmentOrNull(registries, id);
        if (enchantment == null) {
            throw new IllegalArgumentException("Unknown enchantment: " + id);
        }
        return enchantment;
    }
}

