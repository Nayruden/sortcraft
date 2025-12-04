package net.sortcraft.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.sortcraft.category.CategoryLoader;
import net.sortcraft.category.CategoryNode;

import java.util.List;

/**
 * Game tests for category matching logic.
 * Migrated from bootstrap unit tests and extended with tag support tests.
 */
public class CategoryMatchingGameTest {

    // ========== Regex Pattern Tests (Migrated from Bootstrap) ==========

    /**
     * Test that regex patterns correctly match items.
     * Migrated from: CategoryMatchingTest.diamondSwordMatchesSwordsCategory
     */
    @GameTest
    public void regexPatternMatchesSwords(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.SWORDS_REGEX);

        ItemStack diamondSword = new ItemStack(Items.DIAMOND_SWORD);
        List<CategoryNode> matches = CategoryLoader.getMatchingCategories(diamondSword);

        if (matches.isEmpty()) {
            helper.fail(Component.literal("Diamond sword should match swords category"));
            return;
        }
        if (matches.size() != 1 || !"swords".equals(matches.get(0).name)) {
            helper.fail(Component.literal("Diamond sword should match exactly 'swords' category"));
            return;
        }

        helper.succeed();
    }

    /**
     * Test that hierarchy with includes works and categories are returned in priority order.
     * Migrated from: CategoryMatchingTest.diamondSwordMatchesMultipleCategoriesByPriority
     */
    @GameTest
    public void hierarchyWithIncludesReturnsPriorityOrder(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.HIERARCHY);

        ItemStack diamondSword = new ItemStack(Items.DIAMOND_SWORD);
        List<CategoryNode> matches = CategoryLoader.getMatchingCategories(diamondSword);

        if (matches.size() != 3) {
            helper.fail(Component.literal("Diamond sword should match 3 categories but got " + matches.size()));
            return;
        }
        if (!"swords".equals(matches.get(0).name)) {
            helper.fail(Component.literal("First match should be 'swords' (priority 10) but got " + matches.get(0).name));
            return;
        }
        if (!"weapons".equals(matches.get(1).name)) {
            helper.fail(Component.literal("Second match should be 'weapons' (priority 15) but got " + matches.get(1).name));
            return;
        }
        if (!"gear".equals(matches.get(2).name)) {
            helper.fail(Component.literal("Third match should be 'gear' (priority 20) but got " + matches.get(2).name));
            return;
        }

        helper.succeed();
    }

    /**
     * Test that explicit item IDs match correctly.
     * Migrated from: CategoryMatchingTest.explicitItemIdMatches
     */
    @GameTest
    public void explicitItemIdMatches(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.SWORDS_SHORT);

        ItemStack diamondSword = new ItemStack(Items.DIAMOND_SWORD);
        ItemStack stoneSword = new ItemStack(Items.STONE_SWORD);

        List<CategoryNode> diamondMatches = CategoryLoader.getMatchingCategories(diamondSword);
        List<CategoryNode> stoneMatches = CategoryLoader.getMatchingCategories(stoneSword);

        if (diamondMatches.size() != 1 || !"swords".equals(diamondMatches.get(0).name)) {
            helper.fail(Component.literal("Diamond sword should match 'swords' category"));
            return;
        }
        if (!stoneMatches.isEmpty()) {
            helper.fail(Component.literal("Stone sword should NOT match (not in SWORDS_SHORT)"));
            return;
        }

        helper.succeed();
    }

    /**
     * Test that non-matching items return empty list.
     * Migrated from: CategoryMatchingTest.nonMatchingItemReturnsEmpty
     */
    @GameTest
    public void nonMatchingItemReturnsEmpty(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.SWORDS_REGEX);

        ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        List<CategoryNode> matches = CategoryLoader.getMatchingCategories(pickaxe);

        if (!matches.isEmpty()) {
            helper.fail(Component.literal("Diamond pickaxe should not match swords category"));
            return;
        }

        helper.succeed();
    }

    /**
     * Test that itemCategoryMap is built correctly.
     * Migrated from: CategoryMatchingTest.itemCategoryMapContainsDiamondSword
     */
    @GameTest
    public void itemCategoryMapIsPopulated(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.SWORDS_REGEX);

        ResourceLocation diamondSwordId = BuiltInRegistries.ITEM.getKey(Items.DIAMOND_SWORD);

        if (!CategoryLoader.getItemCategoryMap().containsKey(diamondSwordId)) {
            helper.fail(Component.literal("Item category map should contain diamond_sword"));
            return;
        }

        helper.succeed();
    }

    // ========== Tag Expansion Tests ==========

    /**
     * Test that vanilla log tag expands to include oak_log and other log variants.
     */
    @GameTest
    public void tagExpandsToAllItems(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.LOGS);

        // Oak log should be in #minecraft:logs
        ItemStack oakLog = new ItemStack(Items.OAK_LOG);
        List<CategoryNode> matches = CategoryLoader.getMatchingCategories(oakLog);

        if (matches.isEmpty()) {
            helper.fail(Component.literal("Oak log should match logs category (via #minecraft:logs tag)"));
            return;
        }
        if (!"logs".equals(matches.get(0).name)) {
            helper.fail(Component.literal("Oak log should match 'logs' category"));
            return;
        }

        // Birch log should also be in #minecraft:logs
        ItemStack birchLog = new ItemStack(Items.BIRCH_LOG);
        List<CategoryNode> birchMatches = CategoryLoader.getMatchingCategories(birchLog);

        if (birchMatches.isEmpty()) {
            helper.fail(Component.literal("Birch log should also match logs category"));
            return;
        }

        helper.succeed();
    }

    /**
     * Test that tag-based category works for actual sorting.
     */
    @GameTest
    public void tagBasedCategorySortsItems(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.LOGS);

        BlockPos inputPos = new BlockPos(1, 1, 1);
        BlockPos categoryPos = new BlockPos(3, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);
        TestHelper.placeSingleChest(helper, categoryPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, categoryPos, Direction.NORTH, "logs");

        // Add oak logs to input
        TestHelper.insertItems(helper, inputPos, new ItemStack(Items.OAK_LOG, 32));

        // Execute sort
        TestHelper.executeSort(helper, inputPos);

        // Verify: Input should be empty
        if (!TestHelper.isChestEmpty(helper, inputPos)) {
            helper.fail(Component.literal("Input chest should be empty after sorting logs"));
            return;
        }

        // Verify: Category chest should have the logs
        int logCount = TestHelper.countItemsInChest(helper, categoryPos, Items.OAK_LOG);
        if (logCount != 32) {
            helper.fail(Component.literal("Category chest should have 32 oak logs but has " + logCount));
            return;
        }

        helper.succeed();
    }

    /**
     * Test that invalid/non-existent tags don't crash, just log a warning.
     */
    @GameTest
    public void invalidTagDoesNotCrash(GameTestHelper helper) {
        // This should load without crashing - the invalid tag will log a warning
        TestHelper.setupCategories(TestCategories.INVALID_TAG);

        // The category should exist but have no items
        if (!CategoryLoader.getCategories().containsKey("invalid")) {
            helper.fail(Component.literal("Category 'invalid' should still be created"));
            return;
        }

        // No items should match since the tag doesn't exist
        ItemStack anyItem = new ItemStack(Items.DIAMOND);
        List<CategoryNode> matches = CategoryLoader.getMatchingCategories(anyItem);

        // Diamond shouldn't match (it's not in a nonexistent tag)
        boolean matchesInvalid = matches.stream().anyMatch(c -> "invalid".equals(c.name));
        if (matchesInvalid) {
            helper.fail(Component.literal("Diamond should not match invalid category"));
            return;
        }

        helper.succeed();
    }

    /**
     * Test that categories with both tags and explicit items work correctly.
     */
    @GameTest
    public void combinedTagAndExplicitItemsWork(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.LOGS_AND_STICKS);

        // Oak log should match (from #minecraft:logs tag)
        ItemStack oakLog = new ItemStack(Items.OAK_LOG);
        List<CategoryNode> logMatches = CategoryLoader.getMatchingCategories(oakLog);
        if (logMatches.isEmpty() || !"wood_items".equals(logMatches.get(0).name)) {
            helper.fail(Component.literal("Oak log should match wood_items category"));
            return;
        }

        // Stick should also match (explicit item)
        ItemStack stick = new ItemStack(Items.STICK);
        List<CategoryNode> stickMatches = CategoryLoader.getMatchingCategories(stick);
        if (stickMatches.isEmpty() || !"wood_items".equals(stickMatches.get(0).name)) {
            helper.fail(Component.literal("Stick should match wood_items category"));
            return;
        }

        // Diamond should NOT match
        ItemStack diamond = new ItemStack(Items.DIAMOND);
        List<CategoryNode> diamondMatches = CategoryLoader.getMatchingCategories(diamond);
        if (!diamondMatches.isEmpty()) {
            helper.fail(Component.literal("Diamond should not match wood_items category"));
            return;
        }

        helper.succeed();
    }

    /**
     * Test that planks tag expansion works (another common vanilla tag).
     */
    @GameTest
    public void planksTagExpands(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.PLANKS);

        // Oak planks should be in #minecraft:planks
        ItemStack oakPlanks = new ItemStack(Items.OAK_PLANKS);
        List<CategoryNode> matches = CategoryLoader.getMatchingCategories(oakPlanks);

        if (matches.isEmpty()) {
            helper.fail(Component.literal("Oak planks should match planks category"));
            return;
        }

        // Spruce planks should also match
        ItemStack sprucePlanks = new ItemStack(Items.SPRUCE_PLANKS);
        List<CategoryNode> spruceMatches = CategoryLoader.getMatchingCategories(sprucePlanks);

        if (spruceMatches.isEmpty()) {
            helper.fail(Component.literal("Spruce planks should also match planks category"));
            return;
        }

        helper.succeed();
    }
}

