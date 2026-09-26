package net.sortcraft.gametest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.sortcraft.container.neoforge.NeoForgeItemHandlerStorage;

/**
 * NeoForge GameTests for {@link NeoForgeItemHandlerStorage}, the adapter used for
 * modded storage exposed through the NeoForge item capability. Runs against NeoForge's
 * own slot-based {@link ItemStacksResourceHandler} so the adapter is covered without
 * any third-party mods installed.
 */
public class NeoForgeItemHandlerStorageGameTest {

    private static ItemStacksResourceHandler handlerWith(int slots) {
        return new ItemStacksResourceHandler(slots);
    }

    private static int amount(ItemStacksResourceHandler handler, int slot) {
        return handler.getAmountAsInt(slot);
    }

    private static boolean fail(GameTestHelper helper, boolean condition, String message) {
        if (condition) helper.fail(Component.literal(message));
        return condition;
    }

    /**
     * Insert tops up a slot already holding the same item before using empty slots,
     * and stops at the handler's capacity.
     */
    public void insertMergesThenFillsEmptySlots(GameTestHelper helper) {
        ItemStacksResourceHandler handler = handlerWith(3);
        handler.set(1, ItemResource.of(Items.COBBLESTONE), 60);
        NeoForgeItemHandlerStorage storage = new NeoForgeItemHandlerStorage(handler);

        int inserted = storage.insert(new ItemStack(Items.COBBLESTONE, 10), false);
        if (fail(helper, inserted != 10, "Expected 10 inserted but got " + inserted)) return;
        if (fail(helper, amount(handler, 1) != 64, "Expected existing slot topped up to 64 but was " + amount(handler, 1))) return;
        if (fail(helper, amount(handler, 0) != 6, "Expected remaining 6 in first empty slot but was " + amount(handler, 0))) return;
        if (fail(helper, amount(handler, 2) != 0, "Expected last slot untouched but was " + amount(handler, 2))) return;

        // 3 slots * 64 = 192 capacity, 70 already stored -> only 122 more fit
        int overflow = storage.insert(new ItemStack(Items.COBBLESTONE, 200), false);
        if (fail(helper, overflow != 122, "Expected 122 inserted at capacity but got " + overflow)) return;

        helper.succeed();
    }

    /**
     * Simulated insert and extract report what would move without changing the handler.
     */
    public void simulateDoesNotModifyHandler(GameTestHelper helper) {
        ItemStacksResourceHandler handler = handlerWith(2);
        handler.set(0, ItemResource.of(Items.DIAMOND), 5);
        NeoForgeItemHandlerStorage storage = new NeoForgeItemHandlerStorage(handler);

        int wouldInsert = storage.insert(new ItemStack(Items.DIAMOND, 10), true);
        if (fail(helper, wouldInsert != 10, "Expected simulated insert of 10 but got " + wouldInsert)) return;
        int wouldExtract = storage.extract(new ItemStack(Items.DIAMOND, 3), true);
        if (fail(helper, wouldExtract != 3, "Expected simulated extract of 3 but got " + wouldExtract)) return;

        if (fail(helper, amount(handler, 0) != 5 || amount(handler, 1) != 0,
                "Simulation modified handler: slot0=" + amount(handler, 0) + " slot1=" + amount(handler, 1))) return;

        helper.succeed();
    }

    /**
     * Extract pulls only matching items, across multiple slots, up to the requested count.
     */
    public void extractTakesMatchingItemsAcrossSlots(GameTestHelper helper) {
        ItemStacksResourceHandler handler = handlerWith(3);
        handler.set(0, ItemResource.of(Items.IRON_INGOT), 4);
        handler.set(1, ItemResource.of(Items.GOLD_INGOT), 8);
        handler.set(2, ItemResource.of(Items.IRON_INGOT), 6);
        NeoForgeItemHandlerStorage storage = new NeoForgeItemHandlerStorage(handler);

        int extracted = storage.extract(new ItemStack(Items.IRON_INGOT, 7), false);
        if (fail(helper, extracted != 7, "Expected 7 extracted but got " + extracted)) return;
        if (fail(helper, amount(handler, 0) != 0 || amount(handler, 2) != 3,
                "Expected iron 0/3 after extract but was " + amount(handler, 0) + "/" + amount(handler, 2))) return;
        if (fail(helper, amount(handler, 1) != 8, "Gold slot should be untouched but was " + amount(handler, 1))) return;

        helper.succeed();
    }

    /**
     * The sorting engine shrinks the stacks returned by allStacks(); cleanup() must then
     * extract exactly the consumed amounts from the handler.
     */
    public void allStacksCleanupExtractsConsumedAmounts(GameTestHelper helper) {
        ItemStacksResourceHandler handler = handlerWith(2);
        handler.set(0, ItemResource.of(Items.COBBLESTONE), 32);
        handler.set(1, ItemResource.of(Items.DIRT), 16);
        NeoForgeItemHandlerStorage storage = new NeoForgeItemHandlerStorage(handler);

        for (ItemStack stack : storage.allStacks()) {
            if (stack.is(Items.COBBLESTONE)) stack.shrink(12);
            if (stack.is(Items.DIRT)) stack.shrink(16);
        }
        // Shrinking the snapshot must not touch the handler until cleanup()
        if (fail(helper, amount(handler, 0) != 32 || amount(handler, 1) != 16,
                "Handler changed before cleanup: " + amount(handler, 0) + "/" + amount(handler, 1))) return;

        storage.cleanup();
        if (fail(helper, amount(handler, 0) != 20, "Expected 20 cobblestone after cleanup but was " + amount(handler, 0))) return;
        if (fail(helper, amount(handler, 1) != 0, "Expected dirt slot emptied after cleanup but was " + amount(handler, 1))) return;

        helper.succeed();
    }

    /**
     * getStack() returns a copy: mutating it must not affect the handler.
     */
    public void getStackReturnsCopy(GameTestHelper helper) {
        ItemStacksResourceHandler handler = handlerWith(1);
        handler.set(0, ItemResource.of(Items.EMERALD), 9);
        NeoForgeItemHandlerStorage storage = new NeoForgeItemHandlerStorage(handler);

        if (fail(helper, storage.getSlotCount() != 1, "Expected 1 slot but got " + storage.getSlotCount())) return;
        ItemStack stack = storage.getStack(0);
        if (fail(helper, !stack.is(Items.EMERALD) || stack.getCount() != 9, "Unexpected stack in slot 0: " + stack)) return;

        stack.shrink(9);
        if (fail(helper, amount(handler, 0) != 9, "Mutating getStack() result changed handler to " + amount(handler, 0))) return;
        if (fail(helper, !storage.getStack(5).isEmpty(), "Out-of-range slot should be empty")) return;
        if (fail(helper, !storage.isItemValid(0, new ItemStack(Items.EMERALD)), "Emerald should be valid in slot 0")) return;

        helper.succeed();
    }
}
