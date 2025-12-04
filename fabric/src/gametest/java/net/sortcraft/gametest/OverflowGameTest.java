package net.sortcraft.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.sortcraft.sorting.SortingResults;

/**
 * GameTests for overflow handling.
 * Tests that items return to input chest when category chests are full.
 */
public class OverflowGameTest {

    /**
     * Test that items remain in input when category chest is completely full.
     */
    @GameTest
    public void itemsRemainInInputWhenCategoryFull(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.COBBLESTONE);

        // Setup with category chest full
        SortingTestSetup setup = TestScenarios.inputWithPrefilledCategory(
            helper, "cobblestone", ItemQuantity.full(Items.COBBLESTONE));

        // Add cobblestone to input
        TestHelper.insertItems(helper, setup.inputPos(), new ItemStack(Items.COBBLESTONE, 64));

        // Execute sort
        SortingResults results = TestHelper.executeSort(helper, setup.inputPos());

        // Verify: Input chest should still have the cobblestone (overflow)
        int inputCount = TestHelper.countItemsInChest(helper, setup.inputPos(), Items.COBBLESTONE);
        if (inputCount != 64) {
            helper.fail(Component.literal("Expected 64 cobblestone to remain in input but found " + inputCount));
            return;
        }

        // Verify: Results should indicate overflow
        if (!results.overflowCategories.contains("cobblestone")) {
            helper.fail(Component.literal("Expected 'cobblestone' in overflow categories"));
            return;
        }

        helper.succeed();
    }

    /**
     * Test partial overflow - some items sorted, some remain.
     */
    @GameTest
    public void partialOverflowSomeItemsSorted(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.COBBLESTONE);

        // Setup with category chest almost full (1 slot remaining = 64 capacity)
        SortingTestSetup setup = TestScenarios.inputWithPrefilledCategory(
            helper, "cobblestone", ItemQuantity.stacks(Items.COBBLESTONE, 26));

        // Add 128 cobblestone to input (64 will fit, 64 will overflow)
        TestHelper.insertItems(helper, setup.inputPos(),
            new ItemStack(Items.COBBLESTONE, 64),
            new ItemStack(Items.COBBLESTONE, 64)
        );

        // Execute sort
        SortingResults results = TestHelper.executeSort(helper, setup.inputPos());

        // Verify: Category chest should be full (27 * 64 = 1728)
        int categoryCount = TestHelper.countItemsInChest(helper, setup.categoryPos(), Items.COBBLESTONE);
        if (categoryCount != 27 * 64) {
            helper.fail(Component.literal("Expected category chest to be full but found " + categoryCount));
            return;
        }

        // Verify: Input should have 64 remaining (overflow)
        int inputCount = TestHelper.countItemsInChest(helper, setup.inputPos(), Items.COBBLESTONE);
        if (inputCount != 64) {
            helper.fail(Component.literal("Expected 64 cobblestone overflow in input but found " + inputCount));
            return;
        }

        helper.succeed();
    }

    /**
     * Test that unsortable items (no matching category) remain in input.
     */
    @GameTest
    public void unsortableItemsRemainInInput(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.SWORDS_SHORT);

        SortingTestSetup setup = TestScenarios.inputWithItems(helper, "swords",
            new ItemStack(Items.DEBUG_STICK));

        // Execute sort
        SortingResults results = TestHelper.executeSort(helper, setup.inputPos());

        // Verify: Debug stick should remain in input
        int inputCount = TestHelper.countItemsInChest(helper, setup.inputPos(), Items.DEBUG_STICK);
        if (inputCount != 1) {
            helper.fail(Component.literal("Expected debug_stick to remain in input but found " + inputCount));
            return;
        }

        // Verify: Results should indicate unknown item
        if (results.unknownItems.isEmpty()) {
            helper.fail(Component.literal("Expected unknown items in results"));
            return;
        }

        helper.succeed();
    }

    /**
     * Test mixed items - some sorted, some unsortable.
     */
    @GameTest
    public void mixedItemsSomeUnsortable(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.SWORDS_SHORT);

        SortingTestSetup setup = TestScenarios.inputWithItems(helper, "swords",
            new ItemStack(Items.DIAMOND_SWORD),  // sortable
            new ItemStack(Items.DEBUG_STICK),    // unsortable
            new ItemStack(Items.DIAMOND_SWORD)   // sortable
        );

        // Execute sort
        TestHelper.executeSort(helper, setup.inputPos());

        // Verify: Swords should be in category chest
        int swordCount = TestHelper.countItemsInChest(helper, setup.categoryPos(), Items.DIAMOND_SWORD);
        if (swordCount != 2) {
            helper.fail(Component.literal("Expected 2 swords in category but found " + swordCount));
            return;
        }

        // Verify: Debug stick should remain in input
        int debugCount = TestHelper.countItemsInChest(helper, setup.inputPos(), Items.DEBUG_STICK);
        if (debugCount != 1) {
            helper.fail(Component.literal("Expected debug_stick in input but found " + debugCount));
            return;
        }

        helper.succeed();
    }

    /**
     * Test that empty chest handling works correctly.
     */
    @GameTest
    public void emptyInputChestNoErrors(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.SWORDS_SHORT);

        // Setup with empty input
        SortingTestSetup setup = TestScenarios.basicInputAndCategory(helper, "swords");

        // Execute sort (input is empty)
        SortingResults results = TestHelper.executeSort(helper, setup.inputPos());

        // Verify: No errors, sorted count should be 0
        if (results.sorted != 0) {
            helper.fail(Component.literal("Expected 0 sorted items but got " + results.sorted));
            return;
        }

        helper.succeed();
    }

    // ========== Container State Edge Cases ==========

    /**
     * Test that a full input chest with all unsortable items remains unchanged.
     */
    @GameTest
    public void fullInputChestAllUnsortable(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.SWORDS_SHORT);

        SortingTestSetup setup = TestScenarios.basicInputAndCategory(helper, "swords");

        // Fill input chest completely with unsortable items (debug sticks don't stack)
        for (int i = 0; i < 27; i++) {
            TestHelper.insertItemAt(helper, setup.inputPos(), i, new ItemStack(Items.DEBUG_STICK));
        }

        // Execute sort
        SortingResults results = TestHelper.executeSort(helper, setup.inputPos());

        // Verify: All items should remain in input
        int inputCount = TestHelper.countItemsInChest(helper, setup.inputPos(), Items.DEBUG_STICK);
        if (inputCount != 27) {
            helper.fail(Component.literal("Expected 27 debug sticks to remain but found " + inputCount));
            return;
        }

        // Verify: Category chest should be empty
        TestHelper.assertChestEmpty(helper, setup.categoryPos());

        // Verify: Results should show 0 sorted
        if (results.sorted != 0) {
            helper.fail(Component.literal("Expected 0 sorted but got " + results.sorted));
            return;
        }

        helper.succeed();
    }

    /**
     * Test sorting exactly enough items to fill category chest to capacity.
     */
    @GameTest
    public void sortExactlyFillsCategoryChest(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.COBBLESTONE);

        // Setup with category almost full (1 slot remaining)
        SortingTestSetup setup = TestScenarios.inputWithPrefilledCategory(
            helper, "cobblestone", ItemQuantity.stacks(Items.COBBLESTONE, 26));

        // Add exactly 64 cobblestone to input (will exactly fill the last slot)
        TestHelper.insertItems(helper, setup.inputPos(), new ItemStack(Items.COBBLESTONE, 64));

        // Execute sort
        SortingResults results = TestHelper.executeSort(helper, setup.inputPos());

        // Verify: Category chest should be completely full
        int categoryCount = TestHelper.countItemsInChest(helper, setup.categoryPos(), Items.COBBLESTONE);
        if (categoryCount != 27 * 64) {
            helper.fail(Component.literal("Expected category chest to be full (1728) but found " + categoryCount));
            return;
        }

        // Verify: Input should be empty
        TestHelper.assertChestEmpty(helper, setup.inputPos());

        // Verify: No overflow
        if (!results.overflowCategories.isEmpty()) {
            helper.fail(Component.literal("Expected no overflow but got: " + results.overflowCategories));
            return;
        }

        helper.succeed();
    }

    /**
     * Test sorting when category chest has partial stacks that can be merged.
     */
    @GameTest
    public void sortMergesWithPartialStacksInCategory(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.COBBLESTONE);

        // Setup with category having a partial stack (32 cobblestone)
        SortingTestSetup setup = TestScenarios.inputWithPrefilledCategory(
            helper, "cobblestone", ItemQuantity.partial(Items.COBBLESTONE, 32));

        // Add 32 cobblestone to input (should merge to make 64)
        TestHelper.insertItems(helper, setup.inputPos(), new ItemStack(Items.COBBLESTONE, 32));

        // Execute sort
        TestHelper.executeSort(helper, setup.inputPos());

        // Verify: Category chest should have 64 cobblestone
        int categoryCount = TestHelper.countItemsInChest(helper, setup.categoryPos(), Items.COBBLESTONE);
        if (categoryCount != 64) {
            helper.fail(Component.literal("Expected 64 cobblestone in category but found " + categoryCount));
            return;
        }

        // Verify: Input should be empty
        TestHelper.assertChestEmpty(helper, setup.inputPos());

        helper.succeed();
    }
}
