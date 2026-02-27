package net.sortcraft.container.neoforge;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.sortcraft.container.SortCraftStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * SortCraftStorage implementation wrapping a NeoForge {@link IItemHandler}.
 *
 * <p>IItemHandler is slot-based and supports simulate mode natively.
 * Uses the same snapshot-based {@link #allStacks()}/{@link #cleanup()} pattern
 * as FabricTransferStorage: allStacks() returns mutable copies that the sorting
 * engine can shrink, and cleanup() extracts the consumed amounts.
 *
 * <p>NeoForge convention: {@link IItemHandler#getStackInSlot(int)} returns a reference
 * that MUST NOT be modified directly. All mutations go through insertItem/extractItem.
 */
public class NeoForgeItemHandlerStorage implements SortCraftStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger("sortcraft");

    private final IItemHandler handler;

    // Tracking for allStacks()/cleanup() pattern (per-slot):
    // allStacks() snapshots each slot; cleanup() extracts the difference.
    private List<ItemStack> trackedStacks;
    private int[] originalCounts;

    public NeoForgeItemHandlerStorage(IItemHandler handler) {
        this.handler = handler;
    }

    @Override
    public int getSlotCount() {
        return handler.getSlots();
    }

    @Override
    public ItemStack getStack(int slot) {
        if (slot < 0 || slot >= handler.getSlots()) return ItemStack.EMPTY;
        return handler.getStackInSlot(slot).copy();
    }

    @Override
    public int insert(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return 0;

        int remaining = stack.getCount();
        ItemStack toInsert = stack.copy();

        // First pass: merge with existing stacks of the same item type
        for (int slot = 0; slot < handler.getSlots() && remaining > 0; slot++) {
            ItemStack existing = handler.getStackInSlot(slot);
            if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(stack, existing)) continue;

            toInsert.setCount(remaining);
            ItemStack remainder = handler.insertItem(slot, toInsert, simulate);
            remaining = remainder.isEmpty() ? 0 : remainder.getCount();
        }

        // Second pass: fill empty slots
        for (int slot = 0; slot < handler.getSlots() && remaining > 0; slot++) {
            if (!handler.getStackInSlot(slot).isEmpty()) continue;

            toInsert.setCount(remaining);
            ItemStack remainder = handler.insertItem(slot, toInsert, simulate);
            remaining = remainder.isEmpty() ? 0 : remainder.getCount();
        }

        return stack.getCount() - remaining;
    }

    @Override
    public int extract(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return 0;

        int maxExtract = stack.getCount();
        int extracted = 0;

        for (int slot = 0; slot < handler.getSlots() && extracted < maxExtract; slot++) {
            ItemStack slotStack = handler.getStackInSlot(slot);
            if (slotStack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, slotStack)) continue;

            int toExtract = Math.min(slotStack.getCount(), maxExtract - extracted);
            ItemStack result = handler.extractItem(slot, toExtract, simulate);
            extracted += result.getCount();
        }

        return extracted;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (slot < 0 || slot >= handler.getSlots()) return false;
        return handler.isItemValid(slot, stack);
    }

    @Override
    public Iterable<ItemStack> allStacks() {
        int slots = handler.getSlots();
        trackedStacks = new ArrayList<>(slots);
        originalCounts = new int[slots];

        for (int slot = 0; slot < slots; slot++) {
            ItemStack slotStack = handler.getStackInSlot(slot);
            originalCounts[slot] = slotStack.getCount();
            trackedStacks.add(slotStack.copy()); // Mutable copy the engine can shrink
        }

        return trackedStacks;
    }

    @Override
    public void cleanup() {
        if (trackedStacks == null || originalCounts == null) return;

        for (int slot = 0; slot < trackedStacks.size() && slot < originalCounts.length; slot++) {
            ItemStack current = trackedStacks.get(slot);
            int consumed = originalCounts[slot] - current.getCount();
            if (consumed > 0) {
                ItemStack extracted = handler.extractItem(slot, consumed, false);
                int actualExtracted = extracted.isEmpty() ? 0 : extracted.getCount();
                if (actualExtracted != consumed) {
                    LOGGER.warn("Extraction mismatch during cleanup at slot {}: expected {}, got {}",
                            slot, consumed, actualExtracted);
                }
            }
        }

        trackedStacks = null;
        originalCounts = null;
    }
}

