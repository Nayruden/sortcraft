package net.sortcraft.audit;

/**
 * Immutable record representing an enchantment on an item.
 *
 * @param id    The enchantment identifier (e.g., "minecraft:sharpness")
 * @param level The enchantment level (e.g., 5 for Sharpness V)
 */
public record EnchantmentInfo(String id, int level) {
}

