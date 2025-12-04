package net.sortcraft.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.sortcraft.category.CategoryLoader;
import net.sortcraft.sorting.SortingResults;

/**
 * GameTests for overflow handling.
 * Tests that items return to input chest when category chests are full.
 */
public class OverflowGameTest {

    private static final String COBBLESTONE_CATEGORY = """
        cobblestone:
          items:
          - minecraft:cobblestone
        """;

    private static final String SWORDS_CATEGORY = """
        swords:
          items:
          - minecraft:diamond_sword
        """;

    /**
     * Test that items remain in input when category chest is completely full.
     */
    @GameTest
    public void itemsRemainInInputWhenCategoryFull(GameTestHelper helper) {
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(COBBLESTONE_CATEGORY);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        BlockPos categoryPos = new BlockPos(3, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);
        TestHelper.placeSingleChest(helper, categoryPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, categoryPos, Direction.NORTH, "cobblestone");

        // Fill category chest completely
        for (int i = 0; i < 27; i++) {
            TestHelper.insertItemAt(helper, categoryPos, i, new ItemStack(Items.COBBLESTONE, 64));
        }

        // Add cobblestone to input
        TestHelper.insertItems(helper, inputPos, new ItemStack(Items.COBBLESTONE, 64));

        // Execute sort
        SortingResults results = TestHelper.executeSort(helper, inputPos);

        // Verify: Input chest should still have the cobblestone (overflow)
        int inputCount = TestHelper.countItemsInChest(helper, inputPos, Items.COBBLESTONE);
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
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(COBBLESTONE_CATEGORY);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        BlockPos categoryPos = new BlockPos(3, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);
        TestHelper.placeSingleChest(helper, categoryPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, categoryPos, Direction.NORTH, "cobblestone");

        // Fill category chest except last slot (26 slots full, 1 empty)
        for (int i = 0; i < 26; i++) {
            TestHelper.insertItemAt(helper, categoryPos, i, new ItemStack(Items.COBBLESTONE, 64));
        }

        // Add 128 cobblestone to input (64 will fit, 64 will overflow)
        TestHelper.insertItems(helper, inputPos,
            new ItemStack(Items.COBBLESTONE, 64),
            new ItemStack(Items.COBBLESTONE, 64)
        );

        // Execute sort using the helper (which properly modifies the input chest)
        SortingResults results = TestHelper.executeSort(helper, inputPos);

        // Verify: Category chest should be full (27 * 64 = 1728)
        int categoryCount = TestHelper.countItemsInChest(helper, categoryPos, Items.COBBLESTONE);
        if (categoryCount != 27 * 64) {
            helper.fail(Component.literal("Expected category chest to be full but found " + categoryCount));
            return;
        }

        // Verify: Input should have 64 remaining (overflow)
        int inputCount = TestHelper.countItemsInChest(helper, inputPos, Items.COBBLESTONE);
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
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(SWORDS_CATEGORY);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        BlockPos categoryPos = new BlockPos(3, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);
        TestHelper.placeSingleChest(helper, categoryPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, categoryPos, Direction.NORTH, "swords");

        // Add items with no category (debug_stick)
        TestHelper.insertItems(helper, inputPos, new ItemStack(Items.DEBUG_STICK));

        // Execute sort
        SortingResults results = TestHelper.executeSort(helper, inputPos);

        // Verify: Debug stick should remain in input
        int inputCount = TestHelper.countItemsInChest(helper, inputPos, Items.DEBUG_STICK);
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
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(SWORDS_CATEGORY);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        BlockPos categoryPos = new BlockPos(3, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);
        TestHelper.placeSingleChest(helper, categoryPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, categoryPos, Direction.NORTH, "swords");

        // Add mixed items
        TestHelper.insertItems(helper, inputPos,
            new ItemStack(Items.DIAMOND_SWORD),  // sortable
            new ItemStack(Items.DEBUG_STICK),    // unsortable
            new ItemStack(Items.DIAMOND_SWORD)   // sortable
        );

        // Execute sort
        TestHelper.executeSort(helper, inputPos);

        // Verify: Swords should be in category chest
        int swordCount = TestHelper.countItemsInChest(helper, categoryPos, Items.DIAMOND_SWORD);
        if (swordCount != 2) {
            helper.fail(Component.literal("Expected 2 swords in category but found " + swordCount));
            return;
        }

        // Verify: Debug stick should remain in input
        int debugCount = TestHelper.countItemsInChest(helper, inputPos, Items.DEBUG_STICK);
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
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(SWORDS_CATEGORY);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        BlockPos categoryPos = new BlockPos(3, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);
        TestHelper.placeSingleChest(helper, categoryPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, categoryPos, Direction.NORTH, "swords");

        // Don't add any items - input is empty

        // Execute sort
        SortingResults results = TestHelper.executeSort(helper, inputPos);

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
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(SWORDS_CATEGORY);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        BlockPos categoryPos = new BlockPos(3, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);
        TestHelper.placeSingleChest(helper, categoryPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, categoryPos, Direction.NORTH, "swords");

        // Fill input chest completely with unsortable items
        for (int i = 0; i < 27; i++) {
            TestHelper.insertItemAt(helper, inputPos, i, new ItemStack(Items.DEBUG_STICK));
        }

        // Execute sort
        SortingResults results = TestHelper.executeSort(helper, inputPos);

        // Verify: All items should remain in input
        int inputCount = TestHelper.countItemsInChest(helper, inputPos, Items.DEBUG_STICK);
        if (inputCount != 27) {
            helper.fail(Component.literal("Expected 27 debug sticks to remain but found " + inputCount));
            return;
        }

        // Verify: Category chest should be empty
        TestHelper.assertChestEmpty(helper, categoryPos);

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
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(COBBLESTONE_CATEGORY);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        BlockPos categoryPos = new BlockPos(3, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);
        TestHelper.placeSingleChest(helper, categoryPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, categoryPos, Direction.NORTH, "cobblestone");

        // Fill category chest except last slot
        for (int i = 0; i < 26; i++) {
            TestHelper.insertItemAt(helper, categoryPos, i, new ItemStack(Items.COBBLESTONE, 64));
        }

        // Add exactly 64 cobblestone to input (will exactly fill the last slot)
        TestHelper.insertItems(helper, inputPos, new ItemStack(Items.COBBLESTONE, 64));

        // Execute sort
        SortingResults results = TestHelper.executeSort(helper, inputPos);

        // Verify: Category chest should be completely full
        int categoryCount = TestHelper.countItemsInChest(helper, categoryPos, Items.COBBLESTONE);
        if (categoryCount != 27 * 64) {
            helper.fail(Component.literal("Expected category chest to be full (1728) but found " + categoryCount));
            return;
        }

        // Verify: Input should be empty
        TestHelper.assertChestEmpty(helper, inputPos);

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
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(COBBLESTONE_CATEGORY);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        BlockPos categoryPos = new BlockPos(3, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);
        TestHelper.placeSingleChest(helper, categoryPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, categoryPos, Direction.NORTH, "cobblestone");

        // Put a partial stack (32) in category chest
        TestHelper.insertItems(helper, categoryPos, new ItemStack(Items.COBBLESTONE, 32));

        // Add 32 cobblestone to input (should merge to make 64)
        TestHelper.insertItems(helper, inputPos, new ItemStack(Items.COBBLESTONE, 32));

        // Execute sort
        TestHelper.executeSort(helper, inputPos);

        // Verify: Category chest should have 64 cobblestone
        int categoryCount = TestHelper.countItemsInChest(helper, categoryPos, Items.COBBLESTONE);
        if (categoryCount != 64) {
            helper.fail(Component.literal("Expected 64 cobblestone in category but found " + categoryCount));
            return;
        }

        // Verify: Input should be empty
        TestHelper.assertChestEmpty(helper, inputPos);

        helper.succeed();
    }
}
