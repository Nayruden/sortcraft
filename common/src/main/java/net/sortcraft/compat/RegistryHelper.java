package net.sortcraft.compat;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Registry helper providing convenient lookup methods for items and enchantments.
 */
public final class RegistryHelper {
    private RegistryHelper() {}

    /**
     * Get an item from the registry, throwing if not found.
     */
    public static Item getItemOrThrow(Identifier id) {
        return BuiltInRegistries.ITEM.get(id)
            .orElseThrow(() -> new IllegalArgumentException("Unknown item: " + id))
            .value();
    }

    /**
     * Get an item from the registry, returning null if not found.
     */
    public static Item getItemOrNull(Identifier id) {
        return BuiltInRegistries.ITEM.get(id)
            .map(ref -> ref.value())
            .orElse(null);
    }

    /**
     * Get an item by key, returning the default item if not found.
     */
    public static Item getItemByKey(Identifier id) {
        return BuiltInRegistries.ITEM.getValue(id);
    }

    /**
     * Get an enchantment by its Identifier from the registry access.
     * Returns null if not found.
     */
    public static Enchantment getEnchantmentOrNull(RegistryAccess registries, Identifier id) {
        HolderLookup.RegistryLookup<Enchantment> enchantmentRegistry = registries.lookupOrThrow(Registries.ENCHANTMENT);
        ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, id);
        return enchantmentRegistry.get(key)
            .map(holder -> holder.value())
            .orElse(null);
    }

    /**
     * Get an enchantment by its Identifier from the registry access.
     * Throws if not found.
     */
    public static Enchantment getEnchantmentOrThrow(RegistryAccess registries, Identifier id) {
        Enchantment enchantment = getEnchantmentOrNull(registries, id);
        if (enchantment == null) {
            throw new IllegalArgumentException("Unknown enchantment: " + id);
        }
        return enchantment;
    }
}

