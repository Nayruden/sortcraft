package net.sortcraft.gametest;

import net.minecraft.world.item.Item;

/**
 * Represents a quantity of items as full stacks plus a partial stack.
 * Used for test setup to specify how much of an item should be in a chest.
 * 
 * Constraints:
 * - fullStacks: 0-27 (single chest capacity)
 * - partialStackSize: 0 to (maxStackSize - 1)
 * - If fullStacks == 27, partialStackSize must be 0
 * - Unstackable items (maxStackSize == 1) cannot have partial stacks
 */
public record ItemQuantity(Item item, int fullStacks, int partialStackSize) {

    public ItemQuantity {
        int maxStack = item.getDefaultMaxStackSize();
        
        if (fullStacks < 0 || fullStacks > 27) {
            throw new IllegalArgumentException("fullStacks must be 0-27, got: " + fullStacks);
        }
        
        if (maxStack == 1 && partialStackSize != 0) {
            throw new IllegalArgumentException(
                "Unstackable items (like " + item + ") cannot have partial stacks");
        }
        
        if (partialStackSize < 0 || partialStackSize >= maxStack) {
            throw new IllegalArgumentException(
                "partialStackSize must be 0 to " + (maxStack - 1) + ", got: " + partialStackSize);
        }
        
        if (fullStacks == 27 && partialStackSize > 0) {
            throw new IllegalArgumentException(
                "Cannot have partial stack when chest is full (27 stacks)");
        }
    }

    // ========== Factory Methods ==========

    /**
     * Creates a quantity representing a completely full chest (27 full stacks).
     */
    public static ItemQuantity full(Item item) {
        return new ItemQuantity(item, 27, 0);
    }

    /**
     * Creates a quantity that fills a chest except for the specified remaining capacity.
     * @param remainingCapacity How many items worth of space to leave empty
     */
    public static ItemQuantity fullExcept(Item item, int remainingCapacity) {
        int maxStack = item.getDefaultMaxStackSize();
        int maxCapacity = 27 * maxStack;
        
        if (remainingCapacity < 0 || remainingCapacity > maxCapacity) {
            throw new IllegalArgumentException(
                "remainingCapacity must be 0 to " + maxCapacity + ", got: " + remainingCapacity);
        }
        
        int fillAmount = maxCapacity - remainingCapacity;
        int stacks = fillAmount / maxStack;
        int partial = fillAmount % maxStack;
        
        return new ItemQuantity(item, stacks, partial);
    }

    /**
     * Creates a quantity with only a partial stack (no full stacks).
     * @param count Number of items (must be less than max stack size)
     */
    public static ItemQuantity partial(Item item, int count) {
        if (count == 0) {
            return new ItemQuantity(item, 0, 0);
        }
        return new ItemQuantity(item, 0, count);
    }

    /**
     * Creates a quantity with the specified number of full stacks (no partial).
     * @param count Number of full stacks (0-27)
     */
    public static ItemQuantity stacks(Item item, int count) {
        return new ItemQuantity(item, count, 0);
    }

    /**
     * Creates an empty quantity (0 items).
     */
    public static ItemQuantity empty(Item item) {
        return new ItemQuantity(item, 0, 0);
    }

    // ========== Computed Properties ==========

    /**
     * Returns the total number of items represented by this quantity.
     */
    public int totalItems() {
        return fullStacks * item.getDefaultMaxStackSize() + partialStackSize;
    }

    /**
     * Returns how many more items could fit in a single chest after this quantity.
     */
    public int remainingCapacity() {
        return 27 * item.getDefaultMaxStackSize() - totalItems();
    }

    /**
     * Returns the number of inventory slots this quantity occupies.
     */
    public int slotsUsed() {
        return fullStacks + (partialStackSize > 0 ? 1 : 0);
    }

    /**
     * Returns the maximum stack size for this item.
     */
    public int maxStackSize() {
        return item.getDefaultMaxStackSize();
    }
}

