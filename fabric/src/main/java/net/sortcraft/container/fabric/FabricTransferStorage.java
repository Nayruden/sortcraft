package net.sortcraft.container.fabric;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.world.item.ItemStack;
import net.sortcraft.container.SortCraftStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * SortCraftStorage implementation wrapping a Fabric Transfer API {@link Storage}&lt;{@link ItemVariant}&gt;.
 *
 * <p>Handles both {@link SlottedStorage} (slot-based access) and non-slotted {@link Storage}
 * (view-based access). Uses transactions for all mutations, supporting both
 * real operations (commit) and simulated previews (abort).
 *
 * <p>The {@link #allStacks()} method returns mutable ItemStack snapshots. The sorting engine
 * shrinks these during distribution. {@link #cleanup()} then calculates what was consumed
 * and performs actual extractions via the Transfer API.
 *
 * <p><strong>Concurrency note:</strong> Between {@link #allStacks()} and {@link #cleanup()},
 * the storage is not locked. If another source (hopper, mod) modifies the storage concurrently,
 * the cleanup extraction may find fewer items than expected, potentially leading to item
 * duplication. This is inherent to the snapshot-based design for non-transactional sorting.
 */
public class FabricTransferStorage implements SortCraftStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger("sortcraft");

    private final Storage<ItemVariant> storage;
    private final SlottedStorage<ItemVariant> slottedStorage;

    // Tracking for allStacks()/cleanup() pattern:
    // allStacks() returns mutable snapshots; the sorting engine shrinks them.
    // cleanup() compares originals to current counts and extracts the difference.
    private List<ItemStack> trackedStacks;
    private List<ItemStack> originalStacks;

    public FabricTransferStorage(Storage<ItemVariant> storage) {
        this.storage = storage;
        this.slottedStorage = (storage instanceof SlottedStorage<ItemVariant> ss) ? ss : null;
    }

    @Override
    public int getSlotCount() {
        if (slottedStorage != null) {
            return slottedStorage.getSlots().size();
        }
        // For non-slotted storage, count the views
        int count = 0;
        for (StorageView<ItemVariant> view : storage) {
            count++;
        }
        return count;
    }

    /**
     * {@inheritDoc}
     *
     * <p><strong>Performance note:</strong> For non-slotted storages, this method
     * iterates views sequentially to reach the requested index, making it O(n)
     * per call. Callers needing all contents should prefer {@link #getContents()}
     * or {@link #allStacks()}, which iterate directly in O(n) total.
     */
    @Override
    public ItemStack getStack(int slot) {
        if (slottedStorage != null) {
            List<SingleSlotStorage<ItemVariant>> slots = slottedStorage.getSlots();
            if (slot < 0 || slot >= slots.size()) return ItemStack.EMPTY;
            SingleSlotStorage<ItemVariant> slotStorage = slots.get(slot);
            if (slotStorage.isResourceBlank() || slotStorage.getAmount() <= 0) return ItemStack.EMPTY;
            return slotStorage.getResource().toStack((int) Math.min(slotStorage.getAmount(), Integer.MAX_VALUE));
        }
        // For non-slotted storage, iterate to the requested index
        int i = 0;
        for (StorageView<ItemVariant> view : storage) {
            if (i == slot) {
                if (view.isResourceBlank() || view.getAmount() <= 0) return ItemStack.EMPTY;
                return view.getResource().toStack((int) Math.min(view.getAmount(), Integer.MAX_VALUE));
            }
            i++;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public int insert(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return 0;

        ItemVariant variant = ItemVariant.of(stack);
        long maxAmount = stack.getCount();

        try (Transaction tx = Transaction.openOuter()) {
            long inserted = storage.insert(variant, maxAmount, tx);
            if (!simulate) {
                tx.commit();
            }
            // If simulate, transaction auto-aborts on close
            return (int) Math.min(inserted, Integer.MAX_VALUE);
        }
    }

    @Override
    public int extract(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return 0;

        ItemVariant variant = ItemVariant.of(stack);
        long maxAmount = stack.getCount();

        try (Transaction tx = Transaction.openOuter()) {
            long extracted = storage.extract(variant, maxAmount, tx);
            if (!simulate) {
                tx.commit();
            }
            return (int) Math.min(extracted, Integer.MAX_VALUE);
        }
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (stack.isEmpty()) return false;

        // Transfer API has no direct "canPlace" check.
        // For SlottedStorage: if the slot already holds the same item type, it's valid
        // regardless of remaining capacity.
        if (slottedStorage != null) {
            List<SingleSlotStorage<ItemVariant>> slots = slottedStorage.getSlots();
            if (slot >= 0 && slot < slots.size()) {
                SingleSlotStorage<ItemVariant> slotStorage = slots.get(slot);
                if (!slotStorage.isResourceBlank()
                        && slotStorage.getResource().equals(ItemVariant.of(stack))) {
                    return true;
                }
            }
        }

        // Fallback: use a simulated insert to test if the storage accepts this item type.
        // Note: this may return false if the storage is completely full, even though
        // the item type itself would be valid.
        ItemVariant variant = ItemVariant.of(stack);
        try (Transaction tx = Transaction.openOuter()) {
            long inserted = storage.insert(variant, 1, tx);
            // Don't commit — just checking acceptance
            return inserted > 0;
        }
    }

    @Override
    public Iterable<ItemStack> allStacks() {
        trackedStacks = new ArrayList<>();
        originalStacks = new ArrayList<>();

        // Snapshot current storage contents as mutable ItemStacks
        for (StorageView<ItemVariant> view : storage) {
            if (view.isResourceBlank() || view.getAmount() <= 0) continue;
            int count = (int) Math.min(view.getAmount(), Integer.MAX_VALUE);
            ItemStack stack = view.getResource().toStack(count);
            trackedStacks.add(stack);
            originalStacks.add(stack.copy());
        }

        return trackedStacks;
    }

    @Override
    public void cleanup() {
        if (trackedStacks == null || originalStacks == null) return;

        // Calculate what was consumed (shrunk by sorting engine) and extract via Transfer API
        try (Transaction tx = Transaction.openOuter()) {
            for (int i = 0; i < trackedStacks.size(); i++) {
                ItemStack original = originalStacks.get(i);
                ItemStack current = trackedStacks.get(i);
                int consumed = original.getCount() - current.getCount();
                if (consumed > 0) {
                    ItemVariant variant = ItemVariant.of(original);
                    long actualExtracted = storage.extract(variant, consumed, tx);
                    if (actualExtracted != consumed) {
                        LOGGER.warn("Extraction mismatch during cleanup: expected {} of {}, got {}",
                                consumed, variant.toStack().getDisplayName().getString(), actualExtracted);
                    }
                }
            }
            tx.commit();
        }

        trackedStacks = null;
        originalStacks = null;
    }

    /** {@inheritDoc} */
    @Override
    public List<ItemStack> getContents() {
        // Override default to avoid slot-based iteration for non-slotted storages
        List<ItemStack> contents = new ArrayList<>();
        for (StorageView<ItemVariant> view : storage) {
            if (view.isResourceBlank() || view.getAmount() <= 0) continue;
            int count = (int) Math.min(view.getAmount(), Integer.MAX_VALUE);
            contents.add(view.getResource().toStack(count));
        }
        return contents;
    }
}

