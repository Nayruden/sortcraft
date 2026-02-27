package net.sortcraft.container;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Platform-agnostic storage abstraction for SortCraft.
 *
 * <p>Wraps different storage implementations (vanilla Container, Fabric Transfer API,
 * NeoForge IItemHandler) behind a unified interface that the sorting engine can use.
 *
 * <p>All implementations must handle both reading and writing operations.
 * The {@code simulate} parameter on mutation methods allows preview mode without
 * actually modifying storage contents.
 */
public interface SortCraftStorage {

    /**
     * Returns the number of slots in this storage.
     * For non-slot-based storages, this may return a logical slot count.
     */
    int getSlotCount();

    /**
     * Gets a copy of the item stack in the given slot.
     *
     * @param slot The slot index
     * @return A copy of the ItemStack in that slot, or ItemStack.EMPTY if empty
     */
    ItemStack getStack(int slot);

    /**
     * Inserts items into this storage.
     *
     * <p>The implementation should use optimal insertion strategy (e.g., merging with
     * existing stacks first, then filling empty slots for vanilla containers).
     *
     * @param stack The item stack to insert (not modified)
     * @param simulate If true, only calculate how many items could be inserted without actually inserting
     * @return The number of items that were (or would be) inserted
     */
    int insert(ItemStack stack, boolean simulate);

    /**
     * Extracts items matching the given stack from this storage.
     *
     * @param stack The item stack to match against (item type and components). Count indicates max to extract.
     * @param simulate If true, only calculate how many items could be extracted without actually extracting
     * @return The number of items that were (or would be) extracted
     */
    int extract(ItemStack stack, boolean simulate);

    /**
     * Checks whether the given item can be placed in the specified slot.
     *
     * @param slot The slot index
     * @param stack The item to check
     * @return true if the item is valid for this slot
     */
    boolean isItemValid(int slot, ItemStack stack);

    /**
     * Returns all item stacks in this storage, including empty slots for slot-based storages.
     * Returns actual references to stacks (not copies) for the sorting engine to
     * iterate and shrink during sorting. After iteration, call {@link #cleanup()} to
     * replace any zero-count stacks with {@link ItemStack#EMPTY}.
     *
     * @return An iterable of all ItemStacks in this storage (may include empty stacks)
     */
    Iterable<ItemStack> allStacks();

    /**
     * Cleans up the storage after sorting operations.
     * For vanilla containers, this replaces count-0 stacks with ItemStack.EMPTY.
     * Other implementations may be no-ops.
     */
    void cleanup();

    /**
     * Returns all non-empty item stacks as a list of copies.
     * Useful for reading contents without affecting the storage.
     */
    default List<ItemStack> getContents() {
        List<ItemStack> contents = new ArrayList<>();
        for (int i = 0; i < getSlotCount(); i++) {
            ItemStack stack = getStack(i);
            if (!stack.isEmpty()) {
                contents.add(stack);
            }
        }
        return contents;
    }
}

