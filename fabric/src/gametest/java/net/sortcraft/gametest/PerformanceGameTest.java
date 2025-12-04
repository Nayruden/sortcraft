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
 * GameTests for performance and stress testing.
 * Tests large item counts, many categories, and edge cases.
 */
public class PerformanceGameTest {

    private static final String MULTI_CATEGORY = """
        swords:
          items:
          - minecraft:diamond_sword
          - minecraft:iron_sword
          - minecraft:stone_sword
          - minecraft:wooden_sword
          - minecraft:golden_sword
          - minecraft:netherite_sword
        pickaxes:
          items:
          - minecraft:diamond_pickaxe
          - minecraft:iron_pickaxe
          - minecraft:stone_pickaxe
          - minecraft:wooden_pickaxe
          - minecraft:golden_pickaxe
          - minecraft:netherite_pickaxe
        axes:
          items:
          - minecraft:diamond_axe
          - minecraft:iron_axe
          - minecraft:stone_axe
          - minecraft:wooden_axe
          - minecraft:golden_axe
          - minecraft:netherite_axe
        cobblestone:
          items:
          - minecraft:cobblestone
        """;

    /**
     * Test sorting a full chest (27 stacks of 64 items).
     */
    @GameTest
    public void sortFullChest(GameTestHelper helper) {
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(MULTI_CATEGORY);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        BlockPos categoryPos = new BlockPos(3, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);

        // Create 3-chest stack for category
        TestHelper.setupCategoryStack(helper, categoryPos, 3, Direction.NORTH, Direction.NORTH, "cobblestone");

        // Fill input chest completely with cobblestone
        for (int i = 0; i < 27; i++) {
            TestHelper.insertItemAt(helper, inputPos, i, new ItemStack(Items.COBBLESTONE, 64));
        }

        // Execute sort using the helper (which properly modifies the input chest)
        long startTime = System.currentTimeMillis();
        SortingResults results = TestHelper.executeSort(helper, inputPos);
        long endTime = System.currentTimeMillis();

        // Verify all items sorted
        if (results.sorted != 27 * 64) {
            helper.fail(Component.literal("Expected " + (27 * 64) + " items sorted but got " + results.sorted));
            return;
        }

        // Verify input is empty
        if (!TestHelper.isChestEmpty(helper, inputPos)) {
            helper.fail(Component.literal("Input chest should be empty after sort"));
            return;
        }

        // Log performance (not a failure condition, just informational)
        long duration = endTime - startTime;
        if (duration > 1000) {
            // Warn if sort takes more than 1 second
            helper.fail(Component.literal("Sort took too long: " + duration + "ms"));
            return;
        }

        helper.succeed();
    }

    /**
     * Test sorting with multiple categories.
     */
    @GameTest
    public void sortToMultipleCategories(GameTestHelper helper) {
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(MULTI_CATEGORY);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        BlockPos swordsPos = new BlockPos(3, 1, 1);
        BlockPos pickaxesPos = new BlockPos(5, 1, 1);
        BlockPos axesPos = new BlockPos(7, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);

        TestHelper.placeSingleChest(helper, swordsPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, swordsPos, Direction.NORTH, "swords");

        TestHelper.placeSingleChest(helper, pickaxesPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, pickaxesPos, Direction.NORTH, "pickaxes");

        TestHelper.placeSingleChest(helper, axesPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, axesPos, Direction.NORTH, "axes");

        // Add mixed items
        TestHelper.insertItems(helper, inputPos,
            new ItemStack(Items.DIAMOND_SWORD),
            new ItemStack(Items.DIAMOND_PICKAXE),
            new ItemStack(Items.DIAMOND_AXE),
            new ItemStack(Items.IRON_SWORD),
            new ItemStack(Items.IRON_PICKAXE),
            new ItemStack(Items.IRON_AXE)
        );

        // Execute sort
        TestHelper.executeSort(helper, inputPos, 10);

        // Verify items went to correct categories
        int swordsCount = TestHelper.countItemsInChest(helper, swordsPos, Items.DIAMOND_SWORD) +
                          TestHelper.countItemsInChest(helper, swordsPos, Items.IRON_SWORD);
        int pickaxesCount = TestHelper.countItemsInChest(helper, pickaxesPos, Items.DIAMOND_PICKAXE) +
                            TestHelper.countItemsInChest(helper, pickaxesPos, Items.IRON_PICKAXE);
        int axesCount = TestHelper.countItemsInChest(helper, axesPos, Items.DIAMOND_AXE) +
                        TestHelper.countItemsInChest(helper, axesPos, Items.IRON_AXE);

        if (swordsCount != 2) {
            helper.fail(Component.literal("Expected 2 swords but found " + swordsCount));
            return;
        }
        if (pickaxesCount != 2) {
            helper.fail(Component.literal("Expected 2 pickaxes but found " + pickaxesCount));
            return;
        }
        if (axesCount != 2) {
            helper.fail(Component.literal("Expected 2 axes but found " + axesCount));
            return;
        }

        helper.succeed();
    }

    /**
     * Test sorting with large search radius.
     */
    @GameTest
    public void sortWithLargeSearchRadius(GameTestHelper helper) {
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(MULTI_CATEGORY);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        // Place category chest far away (but within radius)
        BlockPos categoryPos = new BlockPos(10, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);
        TestHelper.placeSingleChest(helper, categoryPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, categoryPos, Direction.NORTH, "swords");

        TestHelper.insertItems(helper, inputPos, new ItemStack(Items.DIAMOND_SWORD));

        // Execute sort with large radius
        TestHelper.executeSort(helper, inputPos, 15);

        // Verify sword was sorted
        int swordCount = TestHelper.countItemsInChest(helper, categoryPos, Items.DIAMOND_SWORD);
        if (swordCount != 1) {
            helper.fail(Component.literal("Expected 1 sword in category but found " + swordCount));
            return;
        }

        helper.succeed();
    }

    /**
     * Test that items outside search radius are not found.
     */
    @GameTest
    public void categoryOutsideRadiusNotFound(GameTestHelper helper) {
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(MULTI_CATEGORY);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        // Place category chest outside radius
        BlockPos categoryPos = new BlockPos(20, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);
        TestHelper.placeSingleChest(helper, categoryPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, categoryPos, Direction.NORTH, "swords");

        TestHelper.insertItems(helper, inputPos, new ItemStack(Items.DIAMOND_SWORD));

        // Execute sort with small radius (category is outside)
        TestHelper.executeSort(helper, inputPos, 5);

        // Verify sword remained in input (category not found)
        int inputCount = TestHelper.countItemsInChest(helper, inputPos, Items.DIAMOND_SWORD);
        if (inputCount != 1) {
            helper.fail(Component.literal("Expected sword to remain in input but found " + inputCount));
            return;
        }

        helper.succeed();
    }

    /**
     * Test sorting many different item types.
     */
    @GameTest
    public void sortManyDifferentItems(GameTestHelper helper) {
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(MULTI_CATEGORY);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        BlockPos swordsPos = new BlockPos(3, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);
        TestHelper.placeSingleChest(helper, swordsPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, swordsPos, Direction.NORTH, "swords");

        // Add all sword types
        TestHelper.insertItems(helper, inputPos,
            new ItemStack(Items.DIAMOND_SWORD),
            new ItemStack(Items.IRON_SWORD),
            new ItemStack(Items.STONE_SWORD),
            new ItemStack(Items.WOODEN_SWORD),
            new ItemStack(Items.GOLDEN_SWORD),
            new ItemStack(Items.NETHERITE_SWORD)
        );

        // Execute sort
        SortingResults results = TestHelper.executeSort(helper, inputPos);

        // Verify all 6 sword types were sorted
        if (results.sorted != 6) {
            helper.fail(Component.literal("Expected 6 items sorted but got " + results.sorted));
            return;
        }

        helper.succeed();
    }

    // ========== Radius Boundary Tests ==========

    /**
     * Test that chest at exactly the radius distance is included.
     */
    @GameTest
    public void chestAtExactRadiusIncluded(GameTestHelper helper) {
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(MULTI_CATEGORY);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        // Place category chest exactly 5 blocks away (radius = 5)
        BlockPos categoryPos = new BlockPos(6, 1, 1);  // 5 blocks in X direction

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);
        TestHelper.placeSingleChest(helper, categoryPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, categoryPos, Direction.NORTH, "swords");

        TestHelper.insertItems(helper, inputPos, new ItemStack(Items.DIAMOND_SWORD));

        // Execute sort with radius exactly matching distance
        TestHelper.executeSort(helper, inputPos, 5);

        // Verify sword was sorted (chest at exact radius should be included)
        int swordCount = TestHelper.countItemsInChest(helper, categoryPos, Items.DIAMOND_SWORD);
        if (swordCount != 1) {
            helper.fail(Component.literal("Expected sword in category at exact radius but found " + swordCount));
            return;
        }

        helper.succeed();
    }

    /**
     * Test that chest at radius+1 is excluded.
     */
    @GameTest
    public void chestBeyondRadiusExcluded(GameTestHelper helper) {
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(MULTI_CATEGORY);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        // Place category chest 6 blocks away (radius = 5, so this is outside)
        BlockPos categoryPos = new BlockPos(7, 1, 1);  // 6 blocks in X direction

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);
        TestHelper.placeSingleChest(helper, categoryPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, categoryPos, Direction.NORTH, "swords");

        TestHelper.insertItems(helper, inputPos, new ItemStack(Items.DIAMOND_SWORD));

        // Execute sort with radius 5 (chest is at 6)
        TestHelper.executeSort(helper, inputPos, 5);

        // Verify sword remained in input (chest beyond radius)
        int inputCount = TestHelper.countItemsInChest(helper, inputPos, Items.DIAMOND_SWORD);
        if (inputCount != 1) {
            helper.fail(Component.literal("Expected sword to remain in input but found " + inputCount));
            return;
        }

        helper.succeed();
    }

    /**
     * Test diagonal distance calculation.
     * Manhattan distance is used, so diagonal is X + Y + Z.
     */
    @GameTest
    public void diagonalDistanceCalculation(GameTestHelper helper) {
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(MULTI_CATEGORY);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        // Place category chest diagonally: 3 blocks X + 3 blocks Z = 6 Manhattan distance
        BlockPos categoryPos = new BlockPos(4, 1, 4);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);
        TestHelper.placeSingleChest(helper, categoryPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, categoryPos, Direction.NORTH, "swords");

        TestHelper.insertItems(helper, inputPos, new ItemStack(Items.DIAMOND_SWORD));

        // Execute sort with radius 6 (should include diagonal chest)
        TestHelper.executeSort(helper, inputPos, 6);

        // Verify sword was sorted
        int swordCount = TestHelper.countItemsInChest(helper, categoryPos, Items.DIAMOND_SWORD);
        if (swordCount != 1) {
            helper.fail(Component.literal("Expected sword in diagonal category but found " + swordCount));
            return;
        }

        helper.succeed();
    }

    /**
     * Test that vertical distance is included in radius calculation.
     */
    @GameTest
    public void verticalDistanceIncludedInRadius(GameTestHelper helper) {
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(MULTI_CATEGORY);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        // Place category chest 3 blocks up and 2 blocks over = 5 Manhattan distance
        BlockPos categoryPos = new BlockPos(3, 4, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);
        TestHelper.placeSingleChest(helper, categoryPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, categoryPos, Direction.NORTH, "swords");

        TestHelper.insertItems(helper, inputPos, new ItemStack(Items.DIAMOND_SWORD));

        // Execute sort with radius 5 (should include vertical chest)
        TestHelper.executeSort(helper, inputPos, 5);

        // Verify sword was sorted
        int swordCount = TestHelper.countItemsInChest(helper, categoryPos, Items.DIAMOND_SWORD);
        if (swordCount != 1) {
            helper.fail(Component.literal("Expected sword in vertical category but found " + swordCount));
            return;
        }

        helper.succeed();
    }
}
