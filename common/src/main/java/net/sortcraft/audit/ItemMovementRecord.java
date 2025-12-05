package net.sortcraft.audit;

import net.minecraft.core.BlockPos;

/**
 * Immutable record representing a single item movement during a sort operation.
 *
 * @param itemId          The item identifier (e.g., "minecraft:iron_ingot")
 * @param quantity        Number of items moved
 * @param category        The category the item was sorted into
 * @param destinationPos  The position of the destination chest
 * @param partial         True if only part of the stack could be moved
 * @param metadata        Optional metadata about the item (enchantments, custom name, potion type)
 */
public record ItemMovementRecord(
        String itemId,
        int quantity,
        String category,
        BlockPos destinationPos,
        boolean partial,
        ItemMetadata metadata
) {
    /**
     * Creates a movement record without metadata (for backward compatibility).
     */
    public ItemMovementRecord(String itemId, int quantity, String category,
                              BlockPos destinationPos, boolean partial) {
        this(itemId, quantity, category, destinationPos, partial, null);
    }
}

