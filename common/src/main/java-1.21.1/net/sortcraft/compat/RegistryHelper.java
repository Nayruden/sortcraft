package net.sortcraft.compat;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Registry helper for Minecraft 1.21.1
 * In 1.21.1, BuiltInRegistries.ITEM.get() returns Item directly
 * In 1.21.1, registries.registryOrThrow() returns Registry<T>
 */
public final class RegistryHelper {
    private RegistryHelper() {}

    /**
     * Get an item from the registry, throwing if not found.
     */
    public static Item getItemOrThrow(ResourceLocation id) {
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == Items.AIR && !id.equals(BuiltInRegistries.ITEM.getKey(Items.AIR))) {
            throw new IllegalArgumentException("Unknown item: " + id);
        }
        return item;
    }

    /**
     * Get an item from the registry, returning null if not found.
     */
    public static Item getItemOrNull(ResourceLocation id) {
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == Items.AIR && !id.equals(BuiltInRegistries.ITEM.getKey(Items.AIR))) {
            return null;
        }
        return item;
    }

    /**
     * Get an item by key, returning the default item if not found.
     */
    public static Item getItemByKey(ResourceLocation id) {
        return BuiltInRegistries.ITEM.get(id);
    }

    /**
     * Get an enchantment by its ResourceLocation from the registry access.
     * Returns null if not found.
     */
    public static Enchantment getEnchantmentOrNull(RegistryAccess registries, ResourceLocation id) {
        Registry<Enchantment> enchantmentRegistry = registries.registryOrThrow(Registries.ENCHANTMENT);
        ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, id);
        return enchantmentRegistry.get(key);
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

