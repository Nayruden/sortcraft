package net.sortcraft.container.neoforge;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.sortcraft.container.SortCraftStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * SortCraftStorage implementation wrapping a NeoForge item {@link ResourceHandler}
 * (the {@code Capabilities.Item.BLOCK} capability).
 *
 * <p>The handler is slot-based. Simulate mode is implemented by running each
 * per-slot operation in its own root {@link Transaction} and only committing it
 * when not simulating — the same semantics as NeoForge's former
 * {@code IItemHandler.of()} adapter (removed in NeoForge 26.3), which this class
 * previously wrapped.
 * Uses the same snapshot-based {@link #allStacks()}/{@link #cleanup()} pattern
 * as FabricTransferStorage: allStacks() returns mutable copies that the sorting
 * engine can shrink, and cleanup() extracts the consumed amounts.
 */
public class NeoForgeItemHandlerStorage implements SortCraftStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger("sortcraft");

    private final ResourceHandler<ItemResource> handler;

    // Tracking for allStacks()/cleanup() pattern (per-slot):
    // allStacks() snapshots each slot; cleanup() extracts the difference.
    private List<ItemStack> trackedStacks;
    private int[] originalCounts;

    public NeoForgeItemHandlerStorage(ResourceHandler<ItemResource> handler) {
        this.handler = handler;
    }

    /** Returns a new stack holding the contents of the given slot (never a live reference). */
    private ItemStack getStackInSlot(int slot) {
        return ItemUtil.getStack(handler, slot);
    }

    /** Inserts into a single slot, returning the stack that did not fit. */
    private ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return ItemUtil.insertItemReturnRemaining(handler, slot, stack, simulate, null);
    }

    /** Extracts up to {@code amount} of whatever is in the slot, returning how many were extracted. */
    private int extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0) return 0;
        ItemResource resource = handler.getResource(slot);
        if (resource.isEmpty()) return 0;
        amount = Math.min(amount, resource.getMaxStackSize());
        try (Transaction tx = Transaction.openRoot()) {
            int extracted = handler.extract(slot, resource, amount, tx);
            if (!simulate) {
                tx.commit();
            }
            return extracted;
        }
    }

    @Override
    public int getSlotCount() {
        return handler.size();
    }

    @Override
    public ItemStack getStack(int slot) {
        if (slot < 0 || slot >= handler.size()) return ItemStack.EMPTY;
        return getStackInSlot(slot);
    }

    @Override
    public int insert(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return 0;

        int remaining = stack.getCount();
        ItemStack toInsert = stack.copy();

        // First pass: merge with existing stacks of the same item type
        for (int slot = 0; slot < handler.size() && remaining > 0; slot++) {
            ItemStack existing = getStackInSlot(slot);
            if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(stack, existing)) continue;

            toInsert.setCount(remaining);
            ItemStack remainder = insertItem(slot, toInsert, simulate);
            remaining = remainder.isEmpty() ? 0 : remainder.getCount();
        }

        // Second pass: fill empty slots
        for (int slot = 0; slot < handler.size() && remaining > 0; slot++) {
            if (!getStackInSlot(slot).isEmpty()) continue;

            toInsert.setCount(remaining);
            ItemStack remainder = insertItem(slot, toInsert, simulate);
            remaining = remainder.isEmpty() ? 0 : remainder.getCount();
        }

        return stack.getCount() - remaining;
    }

    @Override
    public int extract(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return 0;

        int maxExtract = stack.getCount();
        int extracted = 0;

        for (int slot = 0; slot < handler.size() && extracted < maxExtract; slot++) {
            ItemStack slotStack = getStackInSlot(slot);
            if (slotStack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, slotStack)) continue;

            int toExtract = Math.min(slotStack.getCount(), maxExtract - extracted);
            extracted += extractItem(slot, toExtract, simulate);
        }

        return extracted;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (slot < 0 || slot >= handler.size()) return false;
        return handler.isValid(slot, ItemResource.of(stack));
    }

    @Override
    public Iterable<ItemStack> allStacks() {
        int slots = handler.size();
        trackedStacks = new ArrayList<>(slots);
        originalCounts = new int[slots];

        for (int slot = 0; slot < slots; slot++) {
            ItemStack slotStack = getStackInSlot(slot);
            originalCounts[slot] = slotStack.getCount();
            trackedStacks.add(slotStack); // Fresh copy the engine can shrink
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
                int actualExtracted = extractItem(slot, consumed, false);
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

