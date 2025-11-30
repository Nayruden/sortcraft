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
    public static Item getItemOrThrow(ResourceLocation id) {
        return BuiltInRegistries.ITEM.get(id)
            .orElseThrow(() -> new IllegalArgumentException("Unknown item: " + id))
            .value();
    }

    /**
     * Get an item from the registry, returning null if not found.
     */
    public static Item getItemOrNull(ResourceLocation id) {
        return BuiltInRegistries.ITEM.get(id)
            .map(ref -> ref.value())
            .orElse(null);
    }

    /**
     * Get an item by key, returning the default item if not found.
     */
    public static Item getItemByKey(ResourceLocation id) {
        return BuiltInRegistries.ITEM.getValue(id);
    }

    /**
     * Get an enchantment by its ResourceLocation from the registry access.
     * Returns null if not found.
     */
    public static Enchantment getEnchantmentOrNull(RegistryAccess registries, ResourceLocation id) {
        HolderLookup.RegistryLookup<Enchantment> enchantmentRegistry = registries.lookupOrThrow(Registries.ENCHANTMENT);
        ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, id);
        return enchantmentRegistry.get(key)
            .map(holder -> holder.value())
            .orElse(null);
    }

    /**
     * Get an enchantment by its ResourceLocation from the registry access.
     * Throws if not found.
     */
    public static Enchantment getEnchantmentOrThrow(RegistryAccess registries, ResourceLocation id) {
        Enchantment enchantment = getEnchantmentOrNull(registries, id);
        if (enchantment == null) {
            throw new IllegalArgumentException("Unknown enchantment: " + id);
        }
        return enchantment;
    }
}

