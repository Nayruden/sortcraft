package net.sortcraft.audit;

/**
 * Immutable record containing information about a container that was sorted as a unit
 * because it contained uniform contents meeting the threshold.
 *
 * <p>When a container (shulker box or bundle) contains at least N stacks of the same item,
 * the sorting engine sorts the container itself rather than extracting its contents.
 * This record captures information about what was inside that container for audit purposes.
 *
 * @param itemId         The item identifier of the uniform contents (e.g., "minecraft:iron_ingot")
 * @param stackCount     Number of stacks of this item in the container
 * @param totalItemCount Total number of items across all stacks
 */
public record UniformContainerContents(
        String itemId,
        int stackCount,
        int totalItemCount
) {
}

