package net.sortcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.sortcraft.category.CategoryLoader;
import net.sortcraft.category.CategoryNode;

import java.util.List;
import java.util.Map;

/**
 * Game tests for the OR filter logic (Feature 1) and wildcard item patterns (Feature 2).
 */
public class FilterFeatureGameTest {

    // ========== Feature 1: OR Filter Logic - Enchantment ==========

    /**
     * Test 1: OR enchantment filter matches an item with the first enchantment.
     * enchantment: [sharpness, unbreaking] should match a sword with only Sharpness.
     */
    public void orEnchantmentMatchesFirstEnchantment(GameTestHelper helper) {
        TestHelper.setupCategories(helper, TestCategories.OR_ENCHANTMENT_SWORDS);

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        TestHelper.enchant(helper, sword, "minecraft:sharpness", 3);

        List<CategoryNode> matches = CategoryLoader.getMatchingCategories(sword);
        if (matches.isEmpty() || !"enchanted_swords".equals(matches.get(0).name)) {
            helper.fail(Component.literal("Sword with Sharpness should match OR enchantment filter [sharpness, unbreaking]"));
            return;
        }
        helper.succeed();
    }

    /**
     * Test 2: OR enchantment filter matches an item with the second enchantment.
     * enchantment: [sharpness, unbreaking] should match a sword with only Unbreaking.
     */
    public void orEnchantmentMatchesSecondEnchantment(GameTestHelper helper) {
        TestHelper.setupCategories(helper, TestCategories.OR_ENCHANTMENT_SWORDS);

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        TestHelper.enchant(helper, sword, "minecraft:unbreaking", 2);

        List<CategoryNode> matches = CategoryLoader.getMatchingCategories(sword);
        if (matches.isEmpty() || !"enchanted_swords".equals(matches.get(0).name)) {
            helper.fail(Component.literal("Sword with Unbreaking should match OR enchantment filter [sharpness, unbreaking]"));
            return;
        }
        helper.succeed();
    }

    /**
     * Test 3: OR enchantment filter rejects an item with a non-matching enchantment.
     * enchantment: [sharpness, unbreaking] should NOT match a sword with only Fire Aspect.
     */
    public void orEnchantmentRejectsNoMatch(GameTestHelper helper) {
        TestHelper.setupCategories(helper, TestCategories.OR_ENCHANTMENT_SWORDS);

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        TestHelper.enchant(helper, sword, "minecraft:fire_aspect", 1);

        List<CategoryNode> matches = CategoryLoader.getMatchingCategories(sword);
        if (!matches.isEmpty()) {
            helper.fail(Component.literal("Sword with Fire Aspect should NOT match OR filter [sharpness, unbreaking]"));
            return;
        }
        helper.succeed();
    }

    /**
     * Test 4: OR enchantment filter rejects an unenchanted item.
     */
    public void orEnchantmentRejectsUnenchanted(GameTestHelper helper) {
        TestHelper.setupCategories(helper, TestCategories.OR_ENCHANTMENT_SWORDS);

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);

        List<CategoryNode> matches = CategoryLoader.getMatchingCategories(sword);
        if (!matches.isEmpty()) {
            helper.fail(Component.literal("Unenchanted sword should NOT match OR enchantment filter"));
            return;
        }
        helper.succeed();
    }

    // ========== Feature 1: OR Filter Logic - Negation ==========

    /**
     * Test 5: Negated OR rejects an item matching the first enchantment.
     * !enchantment: [sharpness, unbreaking] = NOT(sharpness OR unbreaking)
     * A sword with Sharpness should be rejected.
     */
    public void negatedOrRejectsFirstEnchantment(GameTestHelper helper) {
        TestHelper.setupCategories(helper, TestCategories.NEGATED_OR_ENCHANTMENT_SWORDS);

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        TestHelper.enchant(helper, sword, "minecraft:sharpness", 3);

        List<CategoryNode> matches = CategoryLoader.getMatchingCategories(sword);
        if (!matches.isEmpty()) {
            helper.fail(Component.literal("Sword with Sharpness should be rejected by negated OR [sharpness, unbreaking]"));
            return;
        }
        helper.succeed();
    }

    /**
     * Test 6: Negated OR accepts an item with a non-matching enchantment.
     * Sword with Fire Aspect (has neither sharpness nor unbreaking) should pass.
     */
    public void negatedOrAcceptsNonMatching(GameTestHelper helper) {
        TestHelper.setupCategories(helper, TestCategories.NEGATED_OR_ENCHANTMENT_SWORDS);

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        TestHelper.enchant(helper, sword, "minecraft:fire_aspect", 1);

        List<CategoryNode> matches = CategoryLoader.getMatchingCategories(sword);
        if (matches.isEmpty() || !"plain_swords".equals(matches.get(0).name)) {
            helper.fail(Component.literal("Sword with Fire Aspect should pass negated OR [sharpness, unbreaking]"));
            return;
        }
        helper.succeed();
    }

    /**
     * Test 7: Negated OR accepts an unenchanted item.
     * Plain sword has neither sharpness nor unbreaking, so NOT(false) = true.
     */
    public void negatedOrAcceptsUnenchanted(GameTestHelper helper) {
        TestHelper.setupCategories(helper, TestCategories.NEGATED_OR_ENCHANTMENT_SWORDS);

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);

        List<CategoryNode> matches = CategoryLoader.getMatchingCategories(sword);
        if (matches.isEmpty() || !"plain_swords".equals(matches.get(0).name)) {
            helper.fail(Component.literal("Unenchanted sword should pass negated OR filter"));
            return;
        }
        helper.succeed();
    }

    // ========== Feature 1: OR Filter Logic - Custom Name ==========

    /**
     * Test 8: OR custom_name filter matches either name.
     * custom_name: [Excalibur, Sting] should match a sword named "Excalibur".
     */
    public void orCustomNameMatchesEitherName(GameTestHelper helper) {
        TestHelper.setupCategories(helper, TestCategories.OR_CUSTOM_NAME_SWORDS);

        ItemStack sword = TestHelper.namedStack(Items.DIAMOND_SWORD, "Excalibur");

        List<CategoryNode> matches = CategoryLoader.getMatchingCategories(sword);
        if (matches.isEmpty() || !"named_swords".equals(matches.get(0).name)) {
            helper.fail(Component.literal("Sword named 'Excalibur' should match OR custom_name [Excalibur, Sting]"));
            return;
        }

        // Also test the second name
        ItemStack sword2 = TestHelper.namedStack(Items.IRON_SWORD, "Sting");
        List<CategoryNode> matches2 = CategoryLoader.getMatchingCategories(sword2);
        if (matches2.isEmpty() || !"named_swords".equals(matches2.get(0).name)) {
            helper.fail(Component.literal("Sword named 'Sting' should also match OR custom_name [Excalibur, Sting]"));
            return;
        }

        helper.succeed();
    }

    /**
     * Test 9: OR custom_name filter rejects a non-matching name.
     */
    public void orCustomNameRejectsNoMatch(GameTestHelper helper) {
        TestHelper.setupCategories(helper, TestCategories.OR_CUSTOM_NAME_SWORDS);

        ItemStack sword = TestHelper.namedStack(Items.DIAMOND_SWORD, "Mjolnir");

        List<CategoryNode> matches = CategoryLoader.getMatchingCategories(sword);
        if (!matches.isEmpty()) {
            helper.fail(Component.literal("Sword named 'Mjolnir' should NOT match OR custom_name [Excalibur, Sting]"));
            return;
        }
        helper.succeed();
    }

    // ========== Feature 1: OR Filter Logic - Edge Cases ==========

    /**
     * Test 10: Single-element list behaves identically to scalar value.
     * enchantment: [sharpness] should work the same as enchantment: sharpness.
     */
    public void singleElementListMatchesLikeScalar(GameTestHelper helper) {
        TestHelper.setupCategories(helper, TestCategories.SINGLE_ELEMENT_LIST_SWORDS);

        ItemStack sharpSword = new ItemStack(Items.DIAMOND_SWORD);
        TestHelper.enchant(helper, sharpSword, "minecraft:sharpness", 3);

        List<CategoryNode> matches = CategoryLoader.getMatchingCategories(sharpSword);
        if (matches.isEmpty() || !"sharp_swords".equals(matches.get(0).name)) {
            helper.fail(Component.literal("Single-element list [sharpness] should match like scalar sharpness"));
            return;
        }

        // Unbreaking should NOT match
        ItemStack unbreakingSword = new ItemStack(Items.IRON_SWORD);
        TestHelper.enchant(helper, unbreakingSword, "minecraft:unbreaking", 2);

        List<CategoryNode> noMatch = CategoryLoader.getMatchingCategories(unbreakingSword);
        if (!noMatch.isEmpty()) {
            helper.fail(Component.literal("Single-element list [sharpness] should NOT match Unbreaking"));
            return;
        }

        helper.succeed();
    }

    /**
     * Test 11: AND across filter entries with OR within an entry.
     * Requires: (sharpness OR unbreaking) AND (any custom name).
     */
    public void orFilterWithAndAcrossEntries(GameTestHelper helper) {
        TestHelper.setupCategories(helper, TestCategories.OR_AND_COMBINED);

        // Enchanted + named: should match
        ItemStack namedEnchanted = TestHelper.namedStack(Items.DIAMOND_SWORD, "Excalibur");
        TestHelper.enchant(helper, namedEnchanted, "minecraft:sharpness", 5);

        List<CategoryNode> matches = CategoryLoader.getMatchingCategories(namedEnchanted);
        if (matches.isEmpty() || !"special_swords".equals(matches.get(0).name)) {
            helper.fail(Component.literal("Named + enchanted sword should match AND(OR enchantment, custom_name)"));
            return;
        }

        // Enchanted but NOT named: should NOT match (fails custom_name AND)
        ItemStack enchantedOnly = new ItemStack(Items.DIAMOND_SWORD);
        TestHelper.enchant(helper, enchantedOnly, "minecraft:sharpness", 5);

        List<CategoryNode> noMatch1 = CategoryLoader.getMatchingCategories(enchantedOnly);
        if (!noMatch1.isEmpty()) {
            helper.fail(Component.literal("Enchanted-only sword should NOT match (missing custom_name)"));
            return;
        }

        // Named but NOT enchanted: should NOT match (fails enchantment AND)
        ItemStack namedOnly = TestHelper.namedStack(Items.DIAMOND_SWORD, "Excalibur");

        List<CategoryNode> noMatch2 = CategoryLoader.getMatchingCategories(namedOnly);
        if (!noMatch2.isEmpty()) {
            helper.fail(Component.literal("Named-only sword should NOT match (missing enchantment)"));
            return;
        }

        helper.succeed();
    }

    /**
     * Test 12: Sorting test - OR enchantment filter sorts matching items correctly.
     */
    public void orFilterSortsCorrectly(GameTestHelper helper) {
        TestHelper.setupCategories(helper, TestCategories.OR_ENCHANTMENT_SWORDS);

        SortingTestSetup setup = TestScenarios.basicInputAndCategory(helper, "enchanted_swords");

        // Sword with Sharpness (matches OR)
        ItemStack sharpSword = new ItemStack(Items.DIAMOND_SWORD);
        TestHelper.enchant(helper, sharpSword, "minecraft:sharpness", 3);

        // Sword with Fire Aspect (does NOT match OR)
        ItemStack fireSword = new ItemStack(Items.IRON_SWORD);
        TestHelper.enchant(helper, fireSword, "minecraft:fire_aspect", 1);

        TestHelper.insertItems(helper, setup.inputPos(), sharpSword, fireSword);
        TestHelper.executeSort(helper, setup.inputPos());

        // Sharpness sword should be in category chest
        int categoryCount = TestHelper.countItemsInChest(helper, setup.categoryPos(), Items.DIAMOND_SWORD);
        if (categoryCount != 1) {
            helper.fail(Component.literal("Category chest should have 1 diamond sword (Sharpness) but has " + categoryCount));
            return;
        }

        // Fire Aspect sword should remain in input (unsorted)
        int inputCount = TestHelper.countItemsInChest(helper, setup.inputPos(), Items.IRON_SWORD);
        if (inputCount != 1) {
            helper.fail(Component.literal("Input chest should still have 1 iron sword (Fire Aspect) but has " + inputCount));
            return;
        }

        helper.succeed();
    }

    // ========== Feature 2: Wildcard Item Patterns ==========

    /**
     * Test 13: Wildcard items: ["*"] matches any item type.
     */
    public void wildcardMatchesAnyItem(GameTestHelper helper) {
        TestHelper.setupCategories(helper, TestCategories.WILDCARD_ALL);

        ItemStack cobblestone = new ItemStack(Items.COBBLESTONE);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        ItemStack diamond = new ItemStack(Items.DIAMOND);

        List<CategoryNode> m1 = CategoryLoader.getMatchingCategories(cobblestone);
        List<CategoryNode> m2 = CategoryLoader.getMatchingCategories(sword);
        List<CategoryNode> m3 = CategoryLoader.getMatchingCategories(diamond);

        if (m1.isEmpty() || !"everything".equals(m1.get(0).name)) {
            helper.fail(Component.literal("Cobblestone should match wildcard category"));
            return;
        }
        if (m2.isEmpty() || !"everything".equals(m2.get(0).name)) {
            helper.fail(Component.literal("Diamond sword should match wildcard category"));
            return;
        }
        if (m3.isEmpty() || !"everything".equals(m3.get(0).name)) {
            helper.fail(Component.literal("Diamond should match wildcard category"));
            return;
        }

        helper.succeed();
    }

    /**
     * Test 14: Wildcard + filter only matches items passing the filter.
     * items: ["*"] with enchantment: "*" should match enchanted items but not plain ones.
     */
    public void wildcardWithFilterOnlyMatchesFiltered(GameTestHelper helper) {
        TestHelper.setupCategories(helper, TestCategories.WILDCARD_ENCHANTED);

        // Enchanted sword should match
        ItemStack enchantedSword = new ItemStack(Items.DIAMOND_SWORD);
        TestHelper.enchant(helper, enchantedSword, "minecraft:sharpness", 3);

        List<CategoryNode> matches = CategoryLoader.getMatchingCategories(enchantedSword);
        if (matches.isEmpty() || !"enchanted_all".equals(matches.get(0).name)) {
            helper.fail(Component.literal("Enchanted sword should match wildcard + enchantment filter"));
            return;
        }

        // Plain cobblestone should NOT match (not enchanted)
        ItemStack cobblestone = new ItemStack(Items.COBBLESTONE);
        List<CategoryNode> noMatch = CategoryLoader.getMatchingCategories(cobblestone);
        if (!noMatch.isEmpty()) {
            helper.fail(Component.literal("Plain cobblestone should NOT match wildcard + enchantment filter"));
            return;
        }

        helper.succeed();
    }

    /**
     * Test 15: Sorting test - wildcard category sorts all items.
     */
    public void wildcardSortsAllItems(GameTestHelper helper) {
        TestHelper.setupCategories(helper, TestCategories.WILDCARD_ALL);

        SortingTestSetup setup = TestScenarios.basicInputAndCategory(helper, "everything");

        TestHelper.insertItems(helper, setup.inputPos(),
            new ItemStack(Items.COBBLESTONE, 32),
            new ItemStack(Items.DIAMOND_SWORD),
            new ItemStack(Items.DIAMOND, 16)
        );

        TestHelper.executeSort(helper, setup.inputPos());

        // Input should be empty
        if (!TestHelper.isChestEmpty(helper, setup.inputPos())) {
            helper.fail(Component.literal("Input chest should be empty after wildcard sort"));
            return;
        }

        // Category chest should have all items
        TestHelper.assertChestNotEmpty(helper, setup.categoryPos());

        helper.succeed();
    }

    /**
     * Test 16: Wildcard respects priority - specific category wins over wildcard.
     */
    public void wildcardRespectsPriority(GameTestHelper helper) {
        TestHelper.setupCategories(helper, TestCategories.WILDCARD_PRIORITY);

        Map<String, BlockPos> positions = TestScenarios.multiCategory(helper, "swords", "everything_else");
        BlockPos inputPos = positions.get("input");
        BlockPos swordsPos = positions.get("swords");
        BlockPos everythingPos = positions.get("everything_else");

        TestHelper.insertItems(helper, inputPos,
            new ItemStack(Items.DIAMOND_SWORD),
            new ItemStack(Items.COBBLESTONE, 32)
        );

        TestHelper.executeSort(helper, inputPos);

        // Sword should go to swords category (priority 1)
        int swordCount = TestHelper.countItemsInChest(helper, swordsPos, Items.DIAMOND_SWORD);
        if (swordCount != 1) {
            helper.fail(Component.literal("Swords chest should have 1 diamond sword but has " + swordCount));
            return;
        }

        // Cobblestone should go to wildcard category (priority 20)
        int cobbleCount = TestHelper.countItemsInChest(helper, everythingPos, Items.COBBLESTONE);
        if (cobbleCount != 32) {
            helper.fail(Component.literal("Wildcard chest should have 32 cobblestone but has " + cobbleCount));
            return;
        }

        // Input should be empty
        if (!TestHelper.isChestEmpty(helper, inputPos)) {
            helper.fail(Component.literal("Input chest should be empty after sort"));
            return;
        }

        helper.succeed();
    }

    // ========== Integration: Wildcard + OR Filter ==========

    /**
     * Test 17: Wildcard with OR enchantment filter sorts only matching enchanted items.
     */
    public void wildcardWithOrFilterSortsCorrectly(GameTestHelper helper) {
        TestHelper.setupCategories(helper, TestCategories.WILDCARD_OR_FILTER);

        SortingTestSetup setup = TestScenarios.basicInputAndCategory(helper, "or_enchanted");

        // Pickaxe with Unbreaking (matches OR filter)
        ItemStack enchantedPick = new ItemStack(Items.DIAMOND_PICKAXE);
        TestHelper.enchant(helper, enchantedPick, "minecraft:unbreaking", 3);

        // Plain cobblestone (no enchantment, fails filter)
        ItemStack cobblestone = new ItemStack(Items.COBBLESTONE, 32);

        TestHelper.insertItems(helper, setup.inputPos(), enchantedPick, cobblestone);
        TestHelper.executeSort(helper, setup.inputPos());

        // Enchanted pickaxe should be in category chest
        int pickCount = TestHelper.countItemsInChest(helper, setup.categoryPos(), Items.DIAMOND_PICKAXE);
        if (pickCount != 1) {
            helper.fail(Component.literal("Category chest should have 1 enchanted pickaxe but has " + pickCount));
            return;
        }

        // Cobblestone should remain in input (fails enchantment filter)
        int cobbleCount = TestHelper.countItemsInChest(helper, setup.inputPos(), Items.COBBLESTONE);
        if (cobbleCount != 32) {
            helper.fail(Component.literal("Input should still have 32 cobblestone but has " + cobbleCount));
            return;
        }

        helper.succeed();
    }
}

