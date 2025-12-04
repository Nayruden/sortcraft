package net.sortcraft.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.sortcraft.sorting.SortingResults;

/**
 * GameTests for item distribution behavior.
 * Tests that items merge with existing partial stacks before filling empty slots.
 */
public class ItemDistributionGameTest {

    // ========== Merge with Existing Stacks ==========

    /**
     * Test that items merge with existing partial stacks in category chest.
     */
    @GameTest
    public void itemsMergeWithExistingPartialStacks(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.COBBLESTONE);

        // Setup with category pre-filled with 32 cobblestone
        SortingTestSetup setup = TestScenarios.inputWithPrefilledCategory(
            helper, "cobblestone", ItemQuantity.partial(Items.COBBLESTONE, 32));

        // Add 32 more cobblestone to input
        TestHelper.insertItems(helper, setup.inputPos(), new ItemStack(Items.COBBLESTONE, 32));

        // Execute sort
        TestHelper.executeSort(helper, setup.inputPos());

        // Verify: Slot 0 should now have 64 cobblestone (merged)
        ItemStack slot0 = TestHelper.getItemAt(helper, setup.categoryPos(), 0);
        if (!slot0.is(Items.COBBLESTONE) || slot0.getCount() != 64) {
            helper.fail(Component.literal("Expected 64 cobblestone in slot 0 but found " +
                slot0.getCount() + " of " + slot0.getItem()));
            return;
        }

        // Verify: Slot 1 should be empty (no overflow to new slot)
        ItemStack slot1 = TestHelper.getItemAt(helper, setup.categoryPos(), 1);
        if (!slot1.isEmpty()) {
            helper.fail(Component.literal("Expected slot 1 to be empty but found " + slot1.getItem()));
            return;
        }

        helper.succeed();
    }

    /**
     * Test that overflow goes to new slots after merging.
     */
    @GameTest
    public void overflowGoesToNewSlotsAfterMerging(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.COBBLESTONE);

        // Setup with category pre-filled with 32 cobblestone
        SortingTestSetup setup = TestScenarios.inputWithPrefilledCategory(
            helper, "cobblestone", ItemQuantity.partial(Items.COBBLESTONE, 32));

        // Add 64 more (32 should merge, 32 should go to new slot)
        TestHelper.insertItems(helper, setup.inputPos(), new ItemStack(Items.COBBLESTONE, 64));

        // Execute sort
        TestHelper.executeSort(helper, setup.inputPos());

        // Verify: Slot 0 should have 64 (full stack)
        ItemStack slot0 = TestHelper.getItemAt(helper, setup.categoryPos(), 0);
        if (!slot0.is(Items.COBBLESTONE) || slot0.getCount() != 64) {
            helper.fail(Component.literal("Expected 64 cobblestone in slot 0"));
            return;
        }

        // Verify: Slot 1 should have 32 (overflow)
        ItemStack slot1 = TestHelper.getItemAt(helper, setup.categoryPos(), 1);
        if (!slot1.is(Items.COBBLESTONE) || slot1.getCount() != 32) {
            helper.fail(Component.literal("Expected 32 cobblestone in slot 1 but found " +
                slot1.getCount()));
            return;
        }

        helper.succeed();
    }

    /**
     * Test that non-stackable items go to separate slots.
     */
    @GameTest
    public void nonStackableItemsGoToSeparateSlots(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.SWORDS_SHORT);

        SortingTestSetup setup = TestScenarios.inputWithItems(helper, "swords",
            new ItemStack(Items.DIAMOND_SWORD),
            new ItemStack(Items.DIAMOND_SWORD),
            new ItemStack(Items.DIAMOND_SWORD)
        );

        // Execute sort
        TestHelper.executeSort(helper, setup.inputPos());

        // Verify: 3 swords should be in 3 separate slots
        int swordCount = TestHelper.countItemsInChest(helper, setup.categoryPos(), Items.DIAMOND_SWORD);
        if (swordCount != 3) {
            helper.fail(Component.literal("Expected 3 swords but found " + swordCount));
            return;
        }

        // Verify each slot has exactly 1 sword
        for (int i = 0; i < 3; i++) {
            ItemStack slot = TestHelper.getItemAt(helper, setup.categoryPos(), i);
            if (!slot.is(Items.DIAMOND_SWORD) || slot.getCount() != 1) {
                helper.fail(Component.literal("Expected 1 sword in slot " + i));
                return;
            }
        }

        helper.succeed();
    }

    /**
     * Test that items fill bottom chest first in a vertical stack.
     */
    @GameTest
    public void itemsFillBottomChestFirst(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.COBBLESTONE);

        // Create 2-chest stack
        SortingTestSetup setup = TestScenarios.inputWithCategoryStack(helper, "cobblestone", 2);

        // Add cobblestone to input
        TestHelper.insertItems(helper, setup.inputPos(), new ItemStack(Items.COBBLESTONE, 64));

        // Execute sort
        TestHelper.executeSort(helper, setup.inputPos());

        // Verify: Bottom chest should have the cobblestone
        int bottomCount = TestHelper.countItemsInChest(helper, setup.categoryPos(0), Items.COBBLESTONE);
        if (bottomCount != 64) {
            helper.fail(Component.literal("Expected 64 cobblestone in bottom chest but found " + bottomCount));
            return;
        }

        // Verify: Top chest should be empty
        if (!TestHelper.isChestEmpty(helper, setup.categoryPos(1))) {
            helper.fail(Component.literal("Top chest should be empty"));
            return;
        }

        helper.succeed();
    }

    /**
     * Test that items overflow to upper chests when bottom is full.
     */
    @GameTest
    public void itemsOverflowToUpperChests(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.COBBLESTONE);

        // Create 2-chest stack with bottom chest full
        SortingTestSetup setup = TestScenarios.inputWithPrefilledCategoryStack(
            helper, "cobblestone",
            ItemQuantity.full(Items.COBBLESTONE),  // bottom: full
            ItemQuantity.empty(Items.COBBLESTONE)  // top: empty
        );

        // Add more cobblestone to input
        TestHelper.insertItems(helper, setup.inputPos(), new ItemStack(Items.COBBLESTONE, 64));

        // Execute sort
        TestHelper.executeSort(helper, setup.inputPos());

        // Verify: Top chest should have the overflow
        int topCount = TestHelper.countItemsInChest(helper, setup.categoryPos(1), Items.COBBLESTONE);
        if (topCount != 64) {
            helper.fail(Component.literal("Expected 64 cobblestone in top chest but found " + topCount));
            return;
        }

        helper.succeed();
    }

    // ========== ItemStack Edge Cases ==========

    /**
     * Test that max stack size items (64) are handled correctly.
     */
    @GameTest
    public void maxStackSizeItemsHandledCorrectly(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.COBBLESTONE);

        SortingTestSetup setup = TestScenarios.inputWithItems(helper, "cobblestone",
            new ItemStack(Items.COBBLESTONE, 64),
            new ItemStack(Items.COBBLESTONE, 64),
            new ItemStack(Items.COBBLESTONE, 64)
        );

        // Execute sort
        TestHelper.executeSort(helper, setup.inputPos());

        // Verify: All 192 cobblestone should be in category
        int categoryCount = TestHelper.countItemsInChest(helper, setup.categoryPos(), Items.COBBLESTONE);
        if (categoryCount != 192) {
            helper.fail(Component.literal("Expected 192 cobblestone but found " + categoryCount));
            return;
        }

        // Verify: Input should be empty
        TestHelper.assertChestEmpty(helper, setup.inputPos());

        helper.succeed();
    }

    /**
     * Test that non-standard stack size items (eggs = 16) are handled correctly.
     */
    @GameTest
    public void nonStandardStackSizeEggsHandledCorrectly(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.EGGS);

        // Setup with category pre-filled with 8 eggs
        SortingTestSetup setup = TestScenarios.inputWithPrefilledCategory(
            helper, "eggs", ItemQuantity.partial(Items.EGG, 8));

        // Add 16 eggs to input (8 should merge, 8 should go to new slot)
        TestHelper.insertItems(helper, setup.inputPos(), new ItemStack(Items.EGG, 16));

        // Execute sort
        TestHelper.executeSort(helper, setup.inputPos());

        // Verify: Slot 0 should have 16 eggs (full stack for eggs)
        ItemStack slot0 = TestHelper.getItemAt(helper, setup.categoryPos(), 0);
        if (!slot0.is(Items.EGG) || slot0.getCount() != 16) {
            helper.fail(Component.literal("Expected 16 eggs in slot 0 but found " + slot0.getCount()));
            return;
        }

        // Verify: Slot 1 should have 8 eggs (overflow)
        ItemStack slot1 = TestHelper.getItemAt(helper, setup.categoryPos(), 1);
        if (!slot1.is(Items.EGG) || slot1.getCount() != 8) {
            helper.fail(Component.literal("Expected 8 eggs in slot 1 but found " + slot1.getCount()));
            return;
        }

        helper.succeed();
    }

    /**
     * Test that ender pearls (stack size 16) are handled correctly.
     */
    @GameTest
    public void nonStandardStackSizePearlsHandledCorrectly(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.PEARLS);

        SortingTestSetup setup = TestScenarios.inputWithItems(helper, "pearls",
            new ItemStack(Items.ENDER_PEARL, 16),
            new ItemStack(Items.ENDER_PEARL, 16)
        );

        // Execute sort
        TestHelper.executeSort(helper, setup.inputPos());

        // Verify: Total should be 32 pearls
        int pearlCount = TestHelper.countItemsInChest(helper, setup.categoryPos(), Items.ENDER_PEARL);
        if (pearlCount != 32) {
            helper.fail(Component.literal("Expected 32 ender pearls but found " + pearlCount));
            return;
        }

        // Verify: Should be in 2 slots of 16 each
        ItemStack slot0 = TestHelper.getItemAt(helper, setup.categoryPos(), 0);
        ItemStack slot1 = TestHelper.getItemAt(helper, setup.categoryPos(), 1);
        if (slot0.getCount() != 16 || slot1.getCount() != 16) {
            helper.fail(Component.literal("Expected 16 pearls in each slot"));
            return;
        }

        helper.succeed();
    }

    /**
     * Test that damaged tools don't merge with undamaged tools.
     */
    @GameTest
    public void damagedToolsDontMergeWithUndamaged(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.TOOLS);

        SortingTestSetup setup = TestScenarios.basicInputAndCategory(helper, "tools");

        // Create undamaged pickaxe
        ItemStack undamaged = new ItemStack(Items.DIAMOND_PICKAXE);

        // Create damaged pickaxe
        ItemStack damaged = new ItemStack(Items.DIAMOND_PICKAXE);
        damaged.setDamageValue(100);

        // Add both to input
        TestHelper.insertItems(helper, setup.inputPos(), undamaged, damaged);

        // Execute sort
        TestHelper.executeSort(helper, setup.inputPos());

        // Verify: Both pickaxes should be in category (in separate slots since tools don't stack)
        int pickaxeCount = TestHelper.countItemsInChest(helper, setup.categoryPos(), Items.DIAMOND_PICKAXE);
        if (pickaxeCount != 2) {
            helper.fail(Component.literal("Expected 2 pickaxes but found " + pickaxeCount));
            return;
        }

        // Verify: One should be damaged, one should not
        ItemStack slot0 = TestHelper.getItemAt(helper, setup.categoryPos(), 0);
        ItemStack slot1 = TestHelper.getItemAt(helper, setup.categoryPos(), 1);

        boolean hasDamaged = slot0.getDamageValue() > 0 || slot1.getDamageValue() > 0;
        boolean hasUndamaged = slot0.getDamageValue() == 0 || slot1.getDamageValue() == 0;

        if (!hasDamaged || !hasUndamaged) {
            helper.fail(Component.literal("Expected one damaged and one undamaged pickaxe"));
            return;
        }

        helper.succeed();
    }

    // ========== Multi-Chest Overflow Tests ==========

    /**
     * Test partial slot overflow within a vertical stack.
     * Bottom chest has 10 cobblestone capacity remaining across two partial slots,
     * input has 64 cobblestone. 10 should go to bottom, 54 to top chest.
     */
    @GameTest
    public void partialSlotOverflowInVerticalStack(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.COBBLESTONE);

        // Create 2-chest stack with bottom chest almost full (26 full stacks + 54 partial = 1718 items)
        // This leaves 10 capacity in bottom chest (64-54=10 in slot 26)
        SortingTestSetup setup = TestScenarios.inputWithPrefilledCategoryStack(
            helper, "cobblestone",
            ItemQuantity.fullExcept(Items.COBBLESTONE, 10),  // bottom: 26 full + 54 partial
            ItemQuantity.empty(Items.COBBLESTONE)            // top: empty
        );

        // Add 64 cobblestone to input
        TestHelper.insertItems(helper, setup.inputPos(), new ItemStack(Items.COBBLESTONE, 64));

        // Execute sort
        TestHelper.executeSort(helper, setup.inputPos());

        // Verify: Bottom chest should now be completely full (1728 = 27 * 64)
        int bottomCount = TestHelper.countItemsInChest(helper, setup.categoryPos(0), Items.COBBLESTONE);
        if (bottomCount != 27 * 64) {
            helper.fail(Component.literal("Expected bottom chest to be full (1728) but found " + bottomCount));
            return;
        }

        // Verify: Top chest should have the overflow (64 - 10 = 54)
        int topCount = TestHelper.countItemsInChest(helper, setup.categoryPos(1), Items.COBBLESTONE);
        if (topCount != 54) {
            helper.fail(Component.literal("Expected 54 cobblestone in top chest but found " + topCount));
            return;
        }

        // Verify: Input should be empty
        TestHelper.assertChestEmpty(helper, setup.inputPos());

        helper.succeed();
    }

    /**
     * Test three-chest vertical stack with overflow across all three.
     * Bottom chest is full, middle has partial capacity, items should overflow to top.
     */
    @GameTest
    public void threeChestStackOverflow(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.COBBLESTONE);

        // Create 3-chest stack: bottom full, middle almost full (32 capacity), top empty
        SortingTestSetup setup = TestScenarios.inputWithPrefilledCategoryStack(
            helper, "cobblestone",
            ItemQuantity.full(Items.COBBLESTONE),             // bottom: full
            ItemQuantity.fullExcept(Items.COBBLESTONE, 32),   // middle: 26 full + 32 partial
            ItemQuantity.empty(Items.COBBLESTONE)             // top: empty
        );

        // Add 64 cobblestone to input (32 should go to middle, 32 to top)
        TestHelper.insertItems(helper, setup.inputPos(), new ItemStack(Items.COBBLESTONE, 64));

        // Execute sort
        TestHelper.executeSort(helper, setup.inputPos());

        // Verify: Middle chest should now be full
        int middleCount = TestHelper.countItemsInChest(helper, setup.categoryPos(1), Items.COBBLESTONE);
        if (middleCount != 27 * 64) {
            helper.fail(Component.literal("Expected middle chest to be full (1728) but found " + middleCount));
            return;
        }

        // Verify: Top chest should have the overflow (32)
        int topCount = TestHelper.countItemsInChest(helper, setup.categoryPos(2), Items.COBBLESTONE);
        if (topCount != 32) {
            helper.fail(Component.literal("Expected 32 cobblestone in top chest but found " + topCount));
            return;
        }

        // Verify: Input should be empty
        TestHelper.assertChestEmpty(helper, setup.inputPos());

        helper.succeed();
    }

    /**
     * Test that items do NOT overflow to a separate category stack.
     * When one category stack is full, items remain in input rather than
     * overflowing to another category stack for the same category.
     * (Cross-stack overflow is not supported - only within a single vertical stack)
     */
    @GameTest
    public void noOverflowToSeparateCategoryStack(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.COBBLESTONE);

        // Create 2-chest stack, both full
        SortingTestSetup setup = TestScenarios.inputWithPrefilledCategoryStack(
            helper, "cobblestone",
            ItemQuantity.full(Items.COBBLESTONE),  // bottom: full
            ItemQuantity.full(Items.COBBLESTONE)   // top: full
        );

        // Create second category chest at position (5, 1, 1)
        BlockPos secondStackPos = new BlockPos(5, 1, 1);
        TestHelper.placeSingleChest(helper, secondStackPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, secondStackPos, Direction.NORTH, "cobblestone");

        // Add cobblestone to input - should NOT overflow to second stack
        TestHelper.insertItems(helper, setup.inputPos(), new ItemStack(Items.COBBLESTONE, 64));

        // Execute sort
        TestHelper.executeSort(helper, setup.inputPos());

        // Verify: Second stack should be empty (no cross-stack overflow)
        int secondStackCount = TestHelper.countItemsInChest(helper, secondStackPos, Items.COBBLESTONE);
        if (secondStackCount != 0) {
            helper.fail(Component.literal("Expected second stack to be empty but found " + secondStackCount));
            return;
        }

        // Verify: Input should still have the 64 cobblestone (could not be sorted)
        int inputCount = TestHelper.countItemsInChest(helper, setup.inputPos(), Items.COBBLESTONE);
        if (inputCount != 64) {
            helper.fail(Component.literal("Expected 64 cobblestone in input but found " + inputCount));
            return;
        }

        helper.succeed();
    }

    /**
     * Test that partial overflow does NOT go to separate category stacks.
     * When first stack has some capacity and second stack exists,
     * only the first stack's capacity is used; rest remains in input.
     */
    @GameTest
    public void noPartialOverflowAcrossMultipleCategoryStacks(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.COBBLESTONE);

        // Create first category chest almost full (32 capacity remaining)
        SortingTestSetup setup = TestScenarios.inputWithPrefilledCategory(
            helper, "cobblestone", ItemQuantity.fullExcept(Items.COBBLESTONE, 32));

        // Create second category chest at position (5, 1, 1)
        BlockPos secondCategoryPos = new BlockPos(5, 1, 1);
        TestHelper.placeSingleChest(helper, secondCategoryPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, secondCategoryPos, Direction.NORTH, "cobblestone");

        // Add 64 cobblestone to input (32 should go to first, 32 should remain in input)
        TestHelper.insertItems(helper, setup.inputPos(), new ItemStack(Items.COBBLESTONE, 64));

        // Execute sort
        TestHelper.executeSort(helper, setup.inputPos());

        // Verify: First chest should now be full
        int firstCount = TestHelper.countItemsInChest(helper, setup.categoryPos(), Items.COBBLESTONE);
        if (firstCount != 27 * 64) {
            helper.fail(Component.literal("Expected first chest to be full (1728) but found " + firstCount));
            return;
        }

        // Verify: Second chest should be empty (no cross-stack overflow)
        int secondCount = TestHelper.countItemsInChest(helper, secondCategoryPos, Items.COBBLESTONE);
        if (secondCount != 0) {
            helper.fail(Component.literal("Expected second chest to be empty but found " + secondCount));
            return;
        }

        // Verify: Input should have the remaining 32 cobblestone (64 - 32 capacity)
        int inputCount = TestHelper.countItemsInChest(helper, setup.inputPos(), Items.COBBLESTONE);
        if (inputCount != 32) {
            helper.fail(Component.literal("Expected 32 cobblestone in input but found " + inputCount));
            return;
        }

        helper.succeed();
    }

    /**
     * Test that a single stack can be split when destination has partial capacity.
     * Input has 64 cobblestone, destination can only fit 10, so 10 go to destination
     * and 54 remain in input as a single stack.
     */
    @GameTest
    public void singleStackSplitOnPartialCapacity(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.COBBLESTONE);

        // Setup with category almost full (10 capacity remaining)
        SortingTestSetup setup = TestScenarios.inputWithPrefilledCategory(
            helper, "cobblestone", ItemQuantity.fullExcept(Items.COBBLESTONE, 10));

        // Add a single stack of 64 cobblestone to input
        TestHelper.insertItems(helper, setup.inputPos(), new ItemStack(Items.COBBLESTONE, 64));

        // Execute sort
        SortingResults results = TestHelper.executeSort(helper, setup.inputPos());

        // Verify: Category chest should now be full
        int categoryCount = TestHelper.countItemsInChest(helper, setup.categoryPos(), Items.COBBLESTONE);
        if (categoryCount != 27 * 64) {
            helper.fail(Component.literal("Expected category chest to be full (1728) but found " + categoryCount));
            return;
        }

        // Verify: Input should have 54 remaining (64 - 10 = 54)
        int inputCount = TestHelper.countItemsInChest(helper, setup.inputPos(), Items.COBBLESTONE);
        if (inputCount != 54) {
            helper.fail(Component.literal("Expected 54 cobblestone in input but found " + inputCount));
            return;
        }

        // Verify: Results should show 10 items sorted
        if (results.sorted != 10) {
            helper.fail(Component.literal("Expected 10 sorted but got " + results.sorted));
            return;
        }

        helper.succeed();
    }
}
