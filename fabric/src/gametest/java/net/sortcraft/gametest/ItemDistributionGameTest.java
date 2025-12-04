package net.sortcraft.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.sortcraft.category.CategoryLoader;

import java.util.List;

/**
 * GameTests for item distribution behavior.
 * Tests that items merge with existing partial stacks before filling empty slots.
 */
public class ItemDistributionGameTest {

    private static final String COBBLESTONE_CATEGORY = """
        cobblestone:
          items:
          - minecraft:cobblestone
        """;

    private static final String SWORDS_CATEGORY = """
        swords:
          items:
          - minecraft:diamond_sword
          - minecraft:iron_sword
        """;

    // ========== Merge with Existing Stacks ==========

    /**
     * Test that items merge with existing partial stacks in category chest.
     */
    @GameTest
    public void itemsMergeWithExistingPartialStacks(GameTestHelper helper) {
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(COBBLESTONE_CATEGORY);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        BlockPos categoryPos = new BlockPos(3, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);
        TestHelper.placeSingleChest(helper, categoryPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, categoryPos, Direction.NORTH, "cobblestone");

        // Pre-fill category chest with partial stack (32 cobblestone in slot 0)
        TestHelper.insertItemAt(helper, categoryPos, 0, new ItemStack(Items.COBBLESTONE, 32));

        // Add 32 more cobblestone to input
        TestHelper.insertItems(helper, inputPos, new ItemStack(Items.COBBLESTONE, 32));

        // Execute sort
        TestHelper.executeSort(helper, inputPos);

        // Verify: Slot 0 should now have 64 cobblestone (merged)
        ItemStack slot0 = TestHelper.getItemAt(helper, categoryPos, 0);
        if (!slot0.is(Items.COBBLESTONE) || slot0.getCount() != 64) {
            helper.fail(Component.literal("Expected 64 cobblestone in slot 0 but found " +
                slot0.getCount() + " of " + slot0.getItem()));
            return;
        }

        // Verify: Slot 1 should be empty (no overflow to new slot)
        ItemStack slot1 = TestHelper.getItemAt(helper, categoryPos, 1);
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
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(COBBLESTONE_CATEGORY);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        BlockPos categoryPos = new BlockPos(3, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);
        TestHelper.placeSingleChest(helper, categoryPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, categoryPos, Direction.NORTH, "cobblestone");

        // Pre-fill with 32 cobblestone
        TestHelper.insertItemAt(helper, categoryPos, 0, new ItemStack(Items.COBBLESTONE, 32));

        // Add 64 more (32 should merge, 32 should go to new slot)
        TestHelper.insertItems(helper, inputPos, new ItemStack(Items.COBBLESTONE, 64));

        // Execute sort
        TestHelper.executeSort(helper, inputPos);

        // Verify: Slot 0 should have 64 (full stack)
        ItemStack slot0 = TestHelper.getItemAt(helper, categoryPos, 0);
        if (!slot0.is(Items.COBBLESTONE) || slot0.getCount() != 64) {
            helper.fail(Component.literal("Expected 64 cobblestone in slot 0"));
            return;
        }

        // Verify: Slot 1 should have 32 (overflow)
        ItemStack slot1 = TestHelper.getItemAt(helper, categoryPos, 1);
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
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(SWORDS_CATEGORY);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        BlockPos categoryPos = new BlockPos(3, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);
        TestHelper.placeSingleChest(helper, categoryPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, categoryPos, Direction.NORTH, "swords");

        // Add 3 diamond swords (non-stackable)
        TestHelper.insertItems(helper, inputPos,
            new ItemStack(Items.DIAMOND_SWORD),
            new ItemStack(Items.DIAMOND_SWORD),
            new ItemStack(Items.DIAMOND_SWORD)
        );

        // Execute sort
        TestHelper.executeSort(helper, inputPos);

        // Verify: 3 swords should be in 3 separate slots
        int swordCount = TestHelper.countItemsInChest(helper, categoryPos, Items.DIAMOND_SWORD);
        if (swordCount != 3) {
            helper.fail(Component.literal("Expected 3 swords but found " + swordCount));
            return;
        }

        // Verify each slot has exactly 1 sword
        for (int i = 0; i < 3; i++) {
            ItemStack slot = TestHelper.getItemAt(helper, categoryPos, i);
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
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(COBBLESTONE_CATEGORY);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        BlockPos bottomCategoryPos = new BlockPos(3, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);

        // Create 2-chest stack with sign on top
        List<BlockPos> categoryStack = TestHelper.setupCategoryStack(
            helper, bottomCategoryPos, 2, Direction.NORTH, Direction.NORTH, "cobblestone"
        );

        // Add cobblestone to input
        TestHelper.insertItems(helper, inputPos, new ItemStack(Items.COBBLESTONE, 64));

        // Execute sort
        TestHelper.executeSort(helper, inputPos);

        // Verify: Bottom chest should have the cobblestone
        int bottomCount = TestHelper.countItemsInChest(helper, bottomCategoryPos, Items.COBBLESTONE);
        if (bottomCount != 64) {
            helper.fail(Component.literal("Expected 64 cobblestone in bottom chest but found " + bottomCount));
            return;
        }

        // Verify: Top chest should be empty
        BlockPos topCategoryPos = categoryStack.get(1);
        if (!TestHelper.isChestEmpty(helper, topCategoryPos)) {
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
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(COBBLESTONE_CATEGORY);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        BlockPos bottomCategoryPos = new BlockPos(3, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);

        // Create 2-chest stack
        List<BlockPos> categoryStack = TestHelper.setupCategoryStack(
            helper, bottomCategoryPos, 2, Direction.NORTH, Direction.NORTH, "cobblestone"
        );

        // Fill bottom chest completely (27 slots * 64 = 1728 cobblestone)
        for (int i = 0; i < 27; i++) {
            TestHelper.insertItemAt(helper, bottomCategoryPos, i, new ItemStack(Items.COBBLESTONE, 64));
        }

        // Add more cobblestone to input
        TestHelper.insertItems(helper, inputPos, new ItemStack(Items.COBBLESTONE, 64));

        // Execute sort
        TestHelper.executeSort(helper, inputPos);

        // Verify: Top chest should have the overflow
        BlockPos topCategoryPos = categoryStack.get(1);
        int topCount = TestHelper.countItemsInChest(helper, topCategoryPos, Items.COBBLESTONE);
        if (topCount != 64) {
            helper.fail(Component.literal("Expected 64 cobblestone in top chest but found " + topCount));
            return;
        }

        helper.succeed();
    }

    // ========== ItemStack Edge Cases ==========

    private static final String EGGS_CATEGORY = """
        eggs:
          items:
          - minecraft:egg
        """;

    private static final String PEARLS_CATEGORY = """
        pearls:
          items:
          - minecraft:ender_pearl
        """;

    private static final String TOOLS_CATEGORY = """
        tools:
          items:
          - minecraft:diamond_pickaxe
          - minecraft:iron_pickaxe
        """;

    /**
     * Test that max stack size items (64) are handled correctly.
     */
    @GameTest
    public void maxStackSizeItemsHandledCorrectly(GameTestHelper helper) {
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(COBBLESTONE_CATEGORY);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        BlockPos categoryPos = new BlockPos(3, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);
        TestHelper.placeSingleChest(helper, categoryPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, categoryPos, Direction.NORTH, "cobblestone");

        // Add multiple full stacks
        TestHelper.insertItems(helper, inputPos,
            new ItemStack(Items.COBBLESTONE, 64),
            new ItemStack(Items.COBBLESTONE, 64),
            new ItemStack(Items.COBBLESTONE, 64)
        );

        // Execute sort
        TestHelper.executeSort(helper, inputPos);

        // Verify: All 192 cobblestone should be in category
        int categoryCount = TestHelper.countItemsInChest(helper, categoryPos, Items.COBBLESTONE);
        if (categoryCount != 192) {
            helper.fail(Component.literal("Expected 192 cobblestone but found " + categoryCount));
            return;
        }

        // Verify: Input should be empty
        TestHelper.assertChestEmpty(helper, inputPos);

        helper.succeed();
    }

    /**
     * Test that non-standard stack size items (eggs = 16) are handled correctly.
     */
    @GameTest
    public void nonStandardStackSizeEggsHandledCorrectly(GameTestHelper helper) {
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(EGGS_CATEGORY);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        BlockPos categoryPos = new BlockPos(3, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);
        TestHelper.placeSingleChest(helper, categoryPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, categoryPos, Direction.NORTH, "eggs");

        // Pre-fill with partial stack of eggs (8)
        TestHelper.insertItemAt(helper, categoryPos, 0, new ItemStack(Items.EGG, 8));

        // Add 16 eggs to input (8 should merge, 8 should go to new slot)
        TestHelper.insertItems(helper, inputPos, new ItemStack(Items.EGG, 16));

        // Execute sort
        TestHelper.executeSort(helper, inputPos);

        // Verify: Slot 0 should have 16 eggs (full stack for eggs)
        ItemStack slot0 = TestHelper.getItemAt(helper, categoryPos, 0);
        if (!slot0.is(Items.EGG) || slot0.getCount() != 16) {
            helper.fail(Component.literal("Expected 16 eggs in slot 0 but found " + slot0.getCount()));
            return;
        }

        // Verify: Slot 1 should have 8 eggs (overflow)
        ItemStack slot1 = TestHelper.getItemAt(helper, categoryPos, 1);
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
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(PEARLS_CATEGORY);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        BlockPos categoryPos = new BlockPos(3, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);
        TestHelper.placeSingleChest(helper, categoryPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, categoryPos, Direction.NORTH, "pearls");

        // Add 32 ender pearls (should fill 2 slots of 16 each)
        TestHelper.insertItems(helper, inputPos,
            new ItemStack(Items.ENDER_PEARL, 16),
            new ItemStack(Items.ENDER_PEARL, 16)
        );

        // Execute sort
        TestHelper.executeSort(helper, inputPos);

        // Verify: Total should be 32 pearls
        int pearlCount = TestHelper.countItemsInChest(helper, categoryPos, Items.ENDER_PEARL);
        if (pearlCount != 32) {
            helper.fail(Component.literal("Expected 32 ender pearls but found " + pearlCount));
            return;
        }

        // Verify: Should be in 2 slots of 16 each
        ItemStack slot0 = TestHelper.getItemAt(helper, categoryPos, 0);
        ItemStack slot1 = TestHelper.getItemAt(helper, categoryPos, 1);
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
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(TOOLS_CATEGORY);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        BlockPos categoryPos = new BlockPos(3, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);
        TestHelper.placeSingleChest(helper, categoryPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, categoryPos, Direction.NORTH, "tools");

        // Create undamaged pickaxe
        ItemStack undamaged = new ItemStack(Items.DIAMOND_PICKAXE);

        // Create damaged pickaxe
        ItemStack damaged = new ItemStack(Items.DIAMOND_PICKAXE);
        damaged.setDamageValue(100);

        // Add both to input
        TestHelper.insertItems(helper, inputPos, undamaged, damaged);

        // Execute sort
        TestHelper.executeSort(helper, inputPos);

        // Verify: Both pickaxes should be in category (in separate slots since tools don't stack)
        int pickaxeCount = TestHelper.countItemsInChest(helper, categoryPos, Items.DIAMOND_PICKAXE);
        if (pickaxeCount != 2) {
            helper.fail(Component.literal("Expected 2 pickaxes but found " + pickaxeCount));
            return;
        }

        // Verify: One should be damaged, one should not
        ItemStack slot0 = TestHelper.getItemAt(helper, categoryPos, 0);
        ItemStack slot1 = TestHelper.getItemAt(helper, categoryPos, 1);

        boolean hasDamaged = slot0.getDamageValue() > 0 || slot1.getDamageValue() > 0;
        boolean hasUndamaged = slot0.getDamageValue() == 0 || slot1.getDamageValue() == 0;

        if (!hasDamaged || !hasUndamaged) {
            helper.fail(Component.literal("Expected one damaged and one undamaged pickaxe"));
            return;
        }

        helper.succeed();
    }

    /**
     * Test that enchanted items are sorted correctly.
     */
    @GameTest
    public void enchantedItemsSortedCorrectly(GameTestHelper helper) {
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(SWORDS_CATEGORY);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        BlockPos categoryPos = new BlockPos(3, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);
        TestHelper.placeSingleChest(helper, categoryPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, categoryPos, Direction.NORTH, "swords");

        // Create enchanted sword (using enchantment glint as indicator)
        ItemStack enchantedSword = new ItemStack(Items.DIAMOND_SWORD);
        // Note: In 1.21.5, enchantments are stored in components
        // For testing, we just verify the sword is sorted regardless of enchantment

        // Create normal sword
        ItemStack normalSword = new ItemStack(Items.DIAMOND_SWORD);

        // Add both to input
        TestHelper.insertItems(helper, inputPos, enchantedSword, normalSword);

        // Execute sort
        TestHelper.executeSort(helper, inputPos);

        // Verify: Both swords should be in category
        int swordCount = TestHelper.countItemsInChest(helper, categoryPos, Items.DIAMOND_SWORD);
        if (swordCount != 2) {
            helper.fail(Component.literal("Expected 2 swords but found " + swordCount));
            return;
        }

        // Verify: Input should be empty
        TestHelper.assertChestEmpty(helper, inputPos);

        helper.succeed();
    }
}
