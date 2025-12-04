package net.sortcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pre-built test scenarios for common sorting test setups.
 * Reduces boilerplate by providing ready-to-use input/category chest configurations.
 */
public final class TestScenarios {
    private TestScenarios() {}

    // Standard positions used by most scenarios
    private static final BlockPos INPUT_POS = new BlockPos(1, 1, 1);
    private static final BlockPos FIRST_CATEGORY_POS = new BlockPos(3, 1, 1);
    private static final Direction DEFAULT_FACING = Direction.NORTH;

    // ========== Simple Scenarios ==========

    /**
     * Sets up a basic input chest and single category chest.
     *
     * @param helper       The GameTestHelper
     * @param categoryName The category name for the sign (without brackets)
     * @return Setup with input at (1,1,1) and category at (3,1,1)
     */
    public static SortingTestSetup basicInputAndCategory(GameTestHelper helper, String categoryName) {
        TestHelper.placeSingleChest(helper, INPUT_POS, DEFAULT_FACING);
        TestHelper.placeInputSign(helper, INPUT_POS, DEFAULT_FACING);
        TestHelper.placeSingleChest(helper, FIRST_CATEGORY_POS, DEFAULT_FACING);
        TestHelper.placeCategorySign(helper, FIRST_CATEGORY_POS, DEFAULT_FACING, categoryName);

        return new SortingTestSetup(INPUT_POS, List.of(FIRST_CATEGORY_POS));
    }

    /**
     * Sets up input and category chests, and inserts items into the input chest.
     *
     * @param helper       The GameTestHelper
     * @param categoryName The category name for the sign
     * @param items        Items to insert into the input chest
     * @return Setup with input at (1,1,1) and category at (3,1,1)
     */
    public static SortingTestSetup inputWithItems(GameTestHelper helper, String categoryName,
                                                   ItemStack... items) {
        SortingTestSetup setup = basicInputAndCategory(helper, categoryName);
        TestHelper.insertItems(helper, setup.inputPos(), items);
        return setup;
    }

    // ========== Multi-Category Scenarios ==========

    /**
     * Sets up an input chest and multiple category chests in a row.
     * Categories are placed at x = 3, 5, 7, ... (spaced 2 blocks apart).
     *
     * @param helper         The GameTestHelper
     * @param categoryNames  Category names in order
     * @return Map where "input" -> input position, and each category name -> its position
     */
    public static Map<String, BlockPos> multiCategory(GameTestHelper helper, String... categoryNames) {
        TestHelper.placeSingleChest(helper, INPUT_POS, DEFAULT_FACING);
        TestHelper.placeInputSign(helper, INPUT_POS, DEFAULT_FACING);

        Map<String, BlockPos> result = new LinkedHashMap<>();
        result.put("input", INPUT_POS);

        int x = 3;
        for (String category : categoryNames) {
            BlockPos pos = new BlockPos(x, 1, 1);
            TestHelper.placeSingleChest(helper, pos, DEFAULT_FACING);
            TestHelper.placeCategorySign(helper, pos, DEFAULT_FACING, category);
            result.put(category, pos);
            x += 2;
        }

        return result;
    }

    // ========== Prefilled Category Scenarios ==========

    /**
     * Sets up input and category chests, with the category chest pre-filled.
     *
     * @param helper       The GameTestHelper
     * @param categoryName The category name
     * @param prefill      ItemQuantity specifying how much to pre-fill
     * @return Setup with input at (1,1,1) and prefilled category at (3,1,1)
     */
    public static SortingTestSetup inputWithPrefilledCategory(GameTestHelper helper,
                                                               String categoryName,
                                                               ItemQuantity prefill) {
        SortingTestSetup setup = basicInputAndCategory(helper, categoryName);
        if (prefill.totalItems() > 0) {
            TestHelper.fillChest(helper, setup.categoryPos(), prefill);
        }
        return setup;
    }

    // ========== Vertical Stack Scenarios ==========

    /**
     * Sets up input chest and an empty vertical stack of category chests.
     *
     * @param helper       The GameTestHelper
     * @param categoryName The category name (sign placed on top chest)
     * @param stackHeight  Number of chests in the vertical stack
     * @return Setup with input at (1,1,1) and category stack starting at (3,1,1)
     */
    public static SortingTestSetup inputWithCategoryStack(GameTestHelper helper,
                                                           String categoryName,
                                                           int stackHeight) {
        TestHelper.placeSingleChest(helper, INPUT_POS, DEFAULT_FACING);
        TestHelper.placeInputSign(helper, INPUT_POS, DEFAULT_FACING);

        List<BlockPos> stackPositions = TestHelper.setupCategoryStack(
            helper, FIRST_CATEGORY_POS, stackHeight, DEFAULT_FACING, DEFAULT_FACING, categoryName
        );

        return new SortingTestSetup(INPUT_POS, stackPositions);
    }

    /**
     * Sets up input chest and a vertical stack of category chests with pre-filled contents.
     * Stack height is inferred from the number of ItemQuantity arguments.
     *
     * @param helper              The GameTestHelper
     * @param categoryName        The category name
     * @param prefillsBottomToTop ItemQuantity for each chest, from bottom to top
     * @return Setup with input and prefilled category stack
     */
    public static SortingTestSetup inputWithPrefilledCategoryStack(GameTestHelper helper,
                                                                    String categoryName,
                                                                    ItemQuantity... prefillsBottomToTop) {
        SortingTestSetup setup = inputWithCategoryStack(helper, categoryName, prefillsBottomToTop.length);

        for (int i = 0; i < prefillsBottomToTop.length; i++) {
            ItemQuantity prefill = prefillsBottomToTop[i];
            if (prefill.totalItems() > 0) {
                TestHelper.fillChest(helper, setup.categoryPos(i), prefill);
            }
        }

        return setup;
    }

    // ========== Nested Container Scenarios ==========

    /**
     * Sets up input chest with nested containers for testing recursive sorting.
     * Input contains:
     * - A shulker box with 2 diamond swords
     * - A bundle with 2 iron swords
     * - 2 loose golden swords
     *
     * @param helper       The GameTestHelper
     * @param categoryName The category name for sorted items
     * @return Setup with input containing nested containers and category chest
     */
    public static SortingTestSetup inputWithNestedContainers(GameTestHelper helper, String categoryName) {
        SortingTestSetup setup = basicInputAndCategory(helper, categoryName);

        // Create shulker with swords
        ItemStack shulker = TestHelper.createShulkerBox(
            new ItemStack(Items.DIAMOND_SWORD),
            new ItemStack(Items.DIAMOND_SWORD)
        );

        // Create bundle with swords
        ItemStack bundle = TestHelper.createBundle(
            new ItemStack(Items.IRON_SWORD),
            new ItemStack(Items.IRON_SWORD)
        );

        // Insert nested containers and loose items
        TestHelper.insertItems(helper, setup.inputPos(),
            shulker,
            bundle,
            new ItemStack(Items.GOLDEN_SWORD),
            new ItemStack(Items.GOLDEN_SWORD)
        );

        return setup;
    }
}

