package net.sortcraft.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.sortcraft.category.CategoryLoader;

/**
 * GameTests for sign text format variations.
 * Tests case sensitivity, bracket handling, multiple lines, front vs back text.
 */
public class SignTextGameTest {

    private static final String SWORDS_CATEGORY = """
        swords:
          items:
          - minecraft:diamond_sword
        """;

    /**
     * Test that category sign text is case-insensitive.
     */
    @GameTest
    public void categorySignCaseInsensitive(GameTestHelper helper) {
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(SWORDS_CATEGORY);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        BlockPos categoryPos = new BlockPos(3, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);
        TestHelper.placeSingleChest(helper, categoryPos, Direction.NORTH);

        // Use uppercase category name
        TestHelper.placeSignOnChest(helper, categoryPos, Direction.NORTH, "[SWORDS]");

        TestHelper.insertItems(helper, inputPos, new ItemStack(Items.DIAMOND_SWORD));

        TestHelper.executeSort(helper, inputPos);

        // Should still sort despite uppercase
        int swordCount = TestHelper.countItemsInChest(helper, categoryPos, Items.DIAMOND_SWORD);
        if (swordCount != 1) {
            helper.fail(Component.literal("Expected 1 sword in category but found " + swordCount));
            return;
        }

        helper.succeed();
    }

    /**
     * Test that mixed case category names work.
     */
    @GameTest
    public void categorySignMixedCase(GameTestHelper helper) {
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(SWORDS_CATEGORY);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        BlockPos categoryPos = new BlockPos(3, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);
        TestHelper.placeSingleChest(helper, categoryPos, Direction.NORTH);

        // Use mixed case
        TestHelper.placeSignOnChest(helper, categoryPos, Direction.NORTH, "[SwOrDs]");

        TestHelper.insertItems(helper, inputPos, new ItemStack(Items.DIAMOND_SWORD));

        TestHelper.executeSort(helper, inputPos);

        int swordCount = TestHelper.countItemsInChest(helper, categoryPos, Items.DIAMOND_SWORD);
        if (swordCount != 1) {
            helper.fail(Component.literal("Expected 1 sword in category but found " + swordCount));
            return;
        }

        helper.succeed();
    }

    /**
     * Test that category text can be on any line of the sign.
     */
    @GameTest
    public void categorySignOnSecondLine(GameTestHelper helper) {
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(SWORDS_CATEGORY);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        BlockPos categoryPos = new BlockPos(3, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);
        TestHelper.placeSingleChest(helper, categoryPos, Direction.NORTH);

        // Place sign with category on second line
        TestHelper.placeSignOnChestMultiLine(helper, categoryPos, Direction.NORTH,
            "Storage", "[swords]", "", "");

        TestHelper.insertItems(helper, inputPos, new ItemStack(Items.DIAMOND_SWORD));

        TestHelper.executeSort(helper, inputPos);

        int swordCount = TestHelper.countItemsInChest(helper, categoryPos, Items.DIAMOND_SWORD);
        if (swordCount != 1) {
            helper.fail(Component.literal("Expected 1 sword in category but found " + swordCount));
            return;
        }

        helper.succeed();
    }

    /**
     * Test that category text can be on the last line of the sign.
     */
    @GameTest
    public void categorySignOnLastLine(GameTestHelper helper) {
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(SWORDS_CATEGORY);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        BlockPos categoryPos = new BlockPos(3, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);
        TestHelper.placeSingleChest(helper, categoryPos, Direction.NORTH);

        // Place sign with category on last line
        TestHelper.placeSignOnChestMultiLine(helper, categoryPos, Direction.NORTH,
            "Line 1", "Line 2", "Line 3", "[swords]");

        TestHelper.insertItems(helper, inputPos, new ItemStack(Items.DIAMOND_SWORD));

        TestHelper.executeSort(helper, inputPos);

        int swordCount = TestHelper.countItemsInChest(helper, categoryPos, Items.DIAMOND_SWORD);
        if (swordCount != 1) {
            helper.fail(Component.literal("Expected 1 sword in category but found " + swordCount));
            return;
        }

        helper.succeed();
    }

    /**
     * Test that input sign text is case-insensitive.
     */
    @GameTest
    public void inputSignCaseInsensitive(GameTestHelper helper) {
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(SWORDS_CATEGORY);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        BlockPos categoryPos = new BlockPos(3, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        // Use uppercase INPUT
        TestHelper.placeSignOnChest(helper, inputPos, Direction.NORTH, "[INPUT]");
        TestHelper.placeSingleChest(helper, categoryPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, categoryPos, Direction.NORTH, "swords");

        TestHelper.insertItems(helper, inputPos, new ItemStack(Items.DIAMOND_SWORD));

        TestHelper.executeSort(helper, inputPos);

        int swordCount = TestHelper.countItemsInChest(helper, categoryPos, Items.DIAMOND_SWORD);
        if (swordCount != 1) {
            helper.fail(Component.literal("Expected 1 sword in category but found " + swordCount));
            return;
        }

        helper.succeed();
    }

    /**
     * Test that sign with extra whitespace still works.
     */
    @GameTest
    public void signWithWhitespace(GameTestHelper helper) {
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(SWORDS_CATEGORY);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        BlockPos categoryPos = new BlockPos(3, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);
        TestHelper.placeSingleChest(helper, categoryPos, Direction.NORTH);

        // Sign text with leading/trailing spaces
        TestHelper.placeSignOnChest(helper, categoryPos, Direction.NORTH, "  [swords]  ");

        TestHelper.insertItems(helper, inputPos, new ItemStack(Items.DIAMOND_SWORD));

        TestHelper.executeSort(helper, inputPos);

        int swordCount = TestHelper.countItemsInChest(helper, categoryPos, Items.DIAMOND_SWORD);
        if (swordCount != 1) {
            helper.fail(Component.literal("Expected 1 sword in category but found " + swordCount));
            return;
        }

        helper.succeed();
    }

    /**
     * Test that sign without brackets doesn't match as category.
     */
    @GameTest
    public void signWithoutBracketsNotCategory(GameTestHelper helper) {
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(SWORDS_CATEGORY);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        BlockPos categoryPos = new BlockPos(3, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);
        TestHelper.placeSingleChest(helper, categoryPos, Direction.NORTH);

        // Sign without brackets - should NOT be recognized as category
        TestHelper.placeSignOnChest(helper, categoryPos, Direction.NORTH, "swords");

        TestHelper.insertItems(helper, inputPos, new ItemStack(Items.DIAMOND_SWORD));

        TestHelper.executeSort(helper, inputPos);

        // Sword should remain in input (no valid category sign)
        int inputCount = TestHelper.countItemsInChest(helper, inputPos, Items.DIAMOND_SWORD);
        if (inputCount != 1) {
            helper.fail(Component.literal("Expected sword to remain in input but found " + inputCount));
            return;
        }

        helper.succeed();
    }

    // ========== Category Priority Tests ==========

    private static final String PRIORITY_CATEGORIES = """
        high_priority:
          priority: 1
          items:
          - minecraft:diamond_sword
        low_priority:
          priority: 20
          items:
          - minecraft:diamond_sword
        """;

    /**
     * Test that items go to higher priority category when both match.
     * Lower priority value = higher priority (checked first).
     */
    @GameTest
    public void itemGoesToHigherPriorityCategory(GameTestHelper helper) {
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(PRIORITY_CATEGORIES);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        BlockPos highPriorityPos = new BlockPos(3, 1, 1);
        BlockPos lowPriorityPos = new BlockPos(5, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);
        TestHelper.placeSingleChest(helper, highPriorityPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, highPriorityPos, Direction.NORTH, "high_priority");
        TestHelper.placeSingleChest(helper, lowPriorityPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, lowPriorityPos, Direction.NORTH, "low_priority");

        TestHelper.insertItems(helper, inputPos, new ItemStack(Items.DIAMOND_SWORD));

        TestHelper.executeSort(helper, inputPos);

        // Sword should go to high_priority (priority 1) not low_priority (priority 20)
        int highCount = TestHelper.countItemsInChest(helper, highPriorityPos, Items.DIAMOND_SWORD);
        int lowCount = TestHelper.countItemsInChest(helper, lowPriorityPos, Items.DIAMOND_SWORD);

        if (highCount != 1) {
            helper.fail(Component.literal("Expected sword in high_priority but found " + highCount));
            return;
        }
        if (lowCount != 0) {
            helper.fail(Component.literal("Expected no sword in low_priority but found " + lowCount));
            return;
        }

        helper.succeed();
    }

    /**
     * Test that items overflow to lower priority category when higher is full.
     */
    @GameTest
    public void itemOverflowsToLowerPriorityCategory(GameTestHelper helper) {
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(PRIORITY_CATEGORIES);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        BlockPos highPriorityPos = new BlockPos(3, 1, 1);
        BlockPos lowPriorityPos = new BlockPos(5, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);
        TestHelper.placeSingleChest(helper, highPriorityPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, highPriorityPos, Direction.NORTH, "high_priority");
        TestHelper.placeSingleChest(helper, lowPriorityPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, lowPriorityPos, Direction.NORTH, "low_priority");

        // Fill high priority chest completely with swords (27 slots)
        for (int i = 0; i < 27; i++) {
            TestHelper.insertItemAt(helper, highPriorityPos, i, new ItemStack(Items.DIAMOND_SWORD));
        }

        // Add one more sword to input
        TestHelper.insertItems(helper, inputPos, new ItemStack(Items.DIAMOND_SWORD));

        TestHelper.executeSort(helper, inputPos);

        // Sword should overflow to low_priority since high_priority is full
        int lowCount = TestHelper.countItemsInChest(helper, lowPriorityPos, Items.DIAMOND_SWORD);
        if (lowCount != 1) {
            helper.fail(Component.literal("Expected sword to overflow to low_priority but found " + lowCount));
            return;
        }

        // Input should be empty
        TestHelper.assertChestEmpty(helper, inputPos);

        helper.succeed();
    }

    /**
     * Test that default priority (10) is used when not specified.
     */
    @GameTest
    public void defaultPriorityIsTen(GameTestHelper helper) {
        String defaultPriorityCategory = """
            default_cat:
              items:
              - minecraft:diamond_sword
            explicit_ten:
              priority: 10
              items:
              - minecraft:iron_sword
            """;
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(defaultPriorityCategory);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        BlockPos defaultPos = new BlockPos(3, 1, 1);
        BlockPos explicitPos = new BlockPos(5, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);
        TestHelper.placeSingleChest(helper, defaultPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, defaultPos, Direction.NORTH, "default_cat");
        TestHelper.placeSingleChest(helper, explicitPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, explicitPos, Direction.NORTH, "explicit_ten");

        // Add both items
        TestHelper.insertItems(helper, inputPos,
            new ItemStack(Items.DIAMOND_SWORD),
            new ItemStack(Items.IRON_SWORD)
        );

        TestHelper.executeSort(helper, inputPos);

        // Both should be sorted to their respective categories
        int diamondCount = TestHelper.countItemsInChest(helper, defaultPos, Items.DIAMOND_SWORD);
        int ironCount = TestHelper.countItemsInChest(helper, explicitPos, Items.IRON_SWORD);

        if (diamondCount != 1) {
            helper.fail(Component.literal("Expected diamond sword in default_cat"));
            return;
        }
        if (ironCount != 1) {
            helper.fail(Component.literal("Expected iron sword in explicit_ten"));
            return;
        }

        helper.succeed();
    }
}
