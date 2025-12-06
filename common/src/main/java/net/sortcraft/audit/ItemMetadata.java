package net.sortcraft.audit;

import java.util.List;

/**
 * Immutable record containing optional metadata about an item.
 * All fields are nullable - only present when the item has that metadata.
 *
 * @param enchantments     List of enchantments on the item, or null if none
 * @param customName       The custom display name (from anvil/commands), or null if none
 * @param potionType       The potion type ID (e.g., "minecraft:strong_healing"), or null if not a potion
 * @param uniformContents  For containers sorted as a unit, info about their uniform contents, or null
 */
public record ItemMetadata(
        List<EnchantmentInfo> enchantments,
        String customName,
        String potionType,
        UniformContainerContents uniformContents
) {
    /**
     * Creates an ItemMetadata without uniform contents (for backward compatibility).
     */
    public ItemMetadata(List<EnchantmentInfo> enchantments, String customName, String potionType) {
        this(enchantments, customName, potionType, null);
    }

    /**
     * Returns true if this metadata has no meaningful content.
     */
    public boolean isEmpty() {
        return (enchantments == null || enchantments.isEmpty())
                && customName == null
                && potionType == null
                && uniformContents == null;
    }

    /**
     * Creates a new ItemMetadata with the given uniform contents added.
     * Preserves any existing enchantments, custom name, and potion type.
     *
     * @param uniformContents The uniform container contents to add
     * @return A new ItemMetadata with uniform contents set
     */
    public ItemMetadata withUniformContents(UniformContainerContents uniformContents) {
        return new ItemMetadata(this.enchantments, this.customName, this.potionType, uniformContents);
    }

    /**
     * Creates a new ItemMetadata containing only uniform contents.
     *
     * @param uniformContents The uniform container contents
     * @return A new ItemMetadata with only uniform contents set
     */
    public static ItemMetadata ofUniformContents(UniformContainerContents uniformContents) {
        return new ItemMetadata(null, null, null, uniformContents);
    }
}

