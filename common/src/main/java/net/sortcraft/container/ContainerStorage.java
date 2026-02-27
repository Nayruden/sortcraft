package net.sortcraft.container;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * SortCraftStorage implementation wrapping a vanilla Minecraft {@link Container}.
 *
 * <p>Handles two-pass insertion (merge with existing stacks, then fill empty slots),
 * extraction, and cleanup of count-0 stacks.
 */
public class ContainerStorage implements SortCraftStorage {

    private final Container container;

    public ContainerStorage(Container container) {
        this.container = container;
    }

    /**
     * Returns the underlying vanilla Container.
     * Used by code that needs direct Container access (e.g., test helpers for direct slot manipulation).
     */
    public Container unwrap() {
        return container;
    }

    @Override
    public int getSlotCount() {
        return container.getContainerSize();
    }

    @Override
    public ItemStack getStack(int slot) {
        return container.getItem(slot).copy();
    }

    @Override
    public int insert(ItemStack stack, boolean simulate) {
        int toInsert = stack.getCount();
        if (toInsert <= 0) return 0;

        int remaining = toInsert;

        // First pass: merge with existing stacks of the same item type
        for (int slot = 0; slot < container.getContainerSize() && remaining > 0; slot++) {
            ItemStack target = container.getItem(slot);
            if (!target.isEmpty()
                    && ItemStack.isSameItemSameComponents(stack, target)
                    && target.getCount() < target.getMaxStackSize()) {
                if (!container.canPlaceItem(slot, stack)) continue;
                int space = target.getMaxStackSize() - target.getCount();
                int move = Math.min(space, remaining);
                if (!simulate) target.grow(move);
                remaining -= move;
            }
        }

        // Second pass: fill empty slots
        for (int slot = 0; slot < container.getContainerSize() && remaining > 0; slot++) {
            if (container.getItem(slot).isEmpty()) {
                if (!container.canPlaceItem(slot, stack)) continue;
                int move = Math.min(remaining, stack.getMaxStackSize());
                if (!simulate) {
                    ItemStack toPut = stack.copy();
                    toPut.setCount(move);
                    container.setItem(slot, toPut);
                }
                remaining -= move;
            }
        }

        return toInsert - remaining;
    }

    @Override
    public int extract(ItemStack stack, boolean simulate) {
        int maxExtract = stack.getCount();
        if (maxExtract <= 0) return 0;

        int extracted = 0;

        for (int slot = 0; slot < container.getContainerSize() && extracted < maxExtract; slot++) {
            ItemStack slotStack = container.getItem(slot);
            if (slotStack.isEmpty()) continue;
            if (!ItemStack.isSameItemSameComponents(stack, slotStack)) continue;

            int toExtract = Math.min(slotStack.getCount(), maxExtract - extracted);
            if (!simulate) {
                slotStack.shrink(toExtract);
                if (slotStack.isEmpty()) {
                    container.setItem(slot, ItemStack.EMPTY);
                }
            }
            extracted += toExtract;
        }

        return extracted;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return container.canPlaceItem(slot, stack);
    }

    @Override
    public Iterable<ItemStack> allStacks() {
        return () -> new Iterator<>() {
            private int index = 0;
            private final int size = container.getContainerSize();

            @Override
            public boolean hasNext() {
                return index < size;
            }

            @Override
            public ItemStack next() {
                if (!hasNext()) {
                    throw new NoSuchElementException("No more items in container");
                }
                return container.getItem(index++);
            }
        };
    }

    @Override
    public void cleanup() {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) {
                container.setItem(i, ItemStack.EMPTY);
            }
        }
    }
}

