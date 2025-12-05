package net.sortcraft.audit;

import java.util.List;

/**
 * Immutable record containing optional metadata about an item.
 * All fields are nullable - only present when the item has that metadata.
 *
 * @param enchantments List of enchantments on the item, or null if none
 * @param customName   The custom display name (from anvil/commands), or null if none
 * @param potionType   The potion type ID (e.g., "minecraft:strong_healing"), or null if not a potion
 */
public record ItemMetadata(
        List<EnchantmentInfo> enchantments,
        String customName,
        String potionType
) {
    /**
     * Returns true if this metadata has no meaningful content.
     */
    public boolean isEmpty() {
        return (enchantments == null || enchantments.isEmpty())
                && customName == null
                && potionType == null;
    }
}

