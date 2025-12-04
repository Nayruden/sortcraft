package net.sortcraft.test;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.sortcraft.category.CategoryLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bootstrap tests for filter functionality.
 * Tests enchantment, custom_name, stackable, and negation filters.
 */
public class FilterTest extends SortCraftBootstrapTestBase {

    // ========== Custom Name Filter ==========

    @Test
    void customNameFilterMatchesExactName() {
        String yaml = """
            named_swords:
              items:
              - minecraft:diamond_sword
              filters:
              - custom_name: "Excalibur"
            """;
        loadCategories(yaml);

        // Create sword with custom name
        ItemStack namedSword = new ItemStack(Items.DIAMOND_SWORD);
        namedSword.set(DataComponents.CUSTOM_NAME, Component.literal("Excalibur"));

        // Create sword without custom name
        ItemStack plainSword = new ItemStack(Items.DIAMOND_SWORD);

        assertStackMatchesCategory(namedSword, "named_swords");
        assertStackDoesNotMatchCategory(plainSword, "named_swords");
    }

    @Test
    void customNameFilterWildcardMatchesAnyName() {
        String yaml = """
            any_named:
              items:
              - minecraft:diamond_sword
              filters:
              - custom_name: "*"
            """;
        loadCategories(yaml);

        ItemStack namedSword = new ItemStack(Items.DIAMOND_SWORD);
        namedSword.set(DataComponents.CUSTOM_NAME, Component.literal("Any Name"));

        ItemStack plainSword = new ItemStack(Items.DIAMOND_SWORD);

        assertStackMatchesCategory(namedSword, "any_named");
        assertStackDoesNotMatchCategory(plainSword, "any_named");
    }

    @Test
    void customNameFilterCaseInsensitive() {
        String yaml = """
            named:
              items:
              - minecraft:diamond_sword
              filters:
              - custom_name: "excalibur"
            """;
        loadCategories(yaml);

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.set(DataComponents.CUSTOM_NAME, Component.literal("EXCALIBUR"));

        assertStackMatchesCategory(sword, "named");
    }

    // ========== Stackable Filter ==========

    @Test
    void stackableFilterMatchesStackableItems() {
        String yaml = """
            stackable:
              items:
              - minecraft:cobblestone
              - minecraft:diamond_sword
              filters:
              - stackable: "true"
            """;
        loadCategories(yaml);

        // Cobblestone is stackable (64)
        assertStackMatchesCategory(new ItemStack(Items.COBBLESTONE), "stackable");

        // Diamond sword is not stackable (1)
        assertStackDoesNotMatchCategory(new ItemStack(Items.DIAMOND_SWORD), "stackable");
    }

    // ========== Negation Filter ==========

    @Test
    void negatedCustomNameFilter() {
        String yaml = """
            unnamed:
              items:
              - minecraft:diamond_sword
              filters:
              - "!custom_name": "*"
            """;
        loadCategories(yaml);

        ItemStack namedSword = new ItemStack(Items.DIAMOND_SWORD);
        namedSword.set(DataComponents.CUSTOM_NAME, Component.literal("Named"));

        ItemStack plainSword = new ItemStack(Items.DIAMOND_SWORD);

        // Negated: matches items WITHOUT custom name
        assertStackMatchesCategory(plainSword, "unnamed");
        assertStackDoesNotMatchCategory(namedSword, "unnamed");
    }

    @Test
    void negatedStackableFilter() {
        String yaml = """
            unstackable:
              items:
              - minecraft:cobblestone
              - minecraft:diamond_sword
              filters:
              - "!stackable": "true"
            """;
        loadCategories(yaml);

        // Negated: matches items that are NOT stackable
        assertStackDoesNotMatchCategory(new ItemStack(Items.COBBLESTONE), "unstackable");
        assertStackMatchesCategory(new ItemStack(Items.DIAMOND_SWORD), "unstackable");
    }

    // ========== Filter Combinations ==========

    @Test
    void multipleFiltersAllMustMatch() {
        String yaml = """
            named_stackable:
              items:
              - minecraft:cobblestone
              filters:
              - custom_name: "*"
              - stackable: "true"
            """;
        loadCategories(yaml);

        ItemStack namedCobble = new ItemStack(Items.COBBLESTONE);
        namedCobble.set(DataComponents.CUSTOM_NAME, Component.literal("Special Cobble"));

        ItemStack plainCobble = new ItemStack(Items.COBBLESTONE);

        // Both filters must match
        assertStackMatchesCategory(namedCobble, "named_stackable");
        assertStackDoesNotMatchCategory(plainCobble, "named_stackable");
    }

    // ========== Edge Cases ==========

    @Test
    void filterOnEmptyStack() {
        String yaml = """
            named:
              items:
              - minecraft:diamond_sword
              filters:
              - custom_name: "*"
            """;
        loadCategories(yaml);

        // Empty stack should not match
        assertStackDoesNotMatchCategory(ItemStack.EMPTY, "named");
    }

    @Test
    void categoryWithoutFilterMatchesAll() {
        String yaml = """
            all_swords:
              items:
              - minecraft:diamond_sword
            """;
        loadCategories(yaml);

        ItemStack namedSword = new ItemStack(Items.DIAMOND_SWORD);
        namedSword.set(DataComponents.CUSTOM_NAME, Component.literal("Named"));

        ItemStack plainSword = new ItemStack(Items.DIAMOND_SWORD);

        // Without filter, both should match
        assertStackMatchesCategory(namedSword, "all_swords");
        assertStackMatchesCategory(plainSword, "all_swords");
    }

    @Test
    void filterWithPriorityOrdering() {
        String yaml = """
            named_swords:
              priority: 100
              items:
              - minecraft:diamond_sword
              filters:
              - custom_name: "*"
            all_swords:
              priority: 10
              items:
              - minecraft:diamond_sword
            """;
        loadCategories(yaml);

        ItemStack namedSword = new ItemStack(Items.DIAMOND_SWORD);
        namedSword.set(DataComponents.CUSTOM_NAME, Component.literal("Named"));

        ItemStack plainSword = new ItemStack(Items.DIAMOND_SWORD);

        // Named sword should match both, with lower priority (10) first
        assertStackMatchesCategoriesInOrder(namedSword, "all_swords", "named_swords");

        // Plain sword should only match all_swords
        assertStackMatchesCategory(plainSword, "all_swords");
        assertStackDoesNotMatchCategory(plainSword, "named_swords");
    }

    // ========== Filter + Include Interaction ==========

    @Test
    void filterAppliedToIncludedCategoryItems() {
        // Filters should apply to items inherited from includes
        String yaml = """
            swords:
              items:
              - minecraft:diamond_sword
              - minecraft:iron_sword
            named_weapons:
              includes:
              - swords
              filters:
              - custom_name: "*"
            """;
        loadCategories(yaml);

        ItemStack namedDiamond = namedStack(Items.DIAMOND_SWORD, "Named");
        ItemStack plainIron = new ItemStack(Items.IRON_SWORD);

        // Both items should match 'swords'
        assertStackMatchesCategory(namedDiamond, "swords");
        assertStackMatchesCategory(plainIron, "swords");

        // Only named item should match 'named_weapons' due to filter
        assertStackMatchesCategory(namedDiamond, "named_weapons");
        assertStackDoesNotMatchCategory(plainIron, "named_weapons");
    }

    @Test
    void filterPriorityWithIncludedCategories() {
        // Test priority ordering when filter categories include other categories
        String yaml = """
            all_swords:
              priority: 100
              items:
              - minecraft:diamond_sword
            named_swords:
              priority: 50
              includes:
              - all_swords
              filters:
              - custom_name: "*"
            """;
        loadCategories(yaml);

        ItemStack namedSword = namedStack(Items.DIAMOND_SWORD, "Named");

        // Should match named_swords (priority 50) before all_swords (priority 100)
        assertStackMatchesCategoriesInOrder(namedSword, "named_swords", "all_swords");
    }

    @Test
    void deepIncludeChainWithFilter() {
        // Test that filters work with deeply nested includes
        String yaml = """
            base:
              items:
              - minecraft:diamond_sword
            middle:
              includes:
              - base
            top:
              includes:
              - middle
              filters:
              - custom_name: "*"
            """;
        loadCategories(yaml);

        ItemStack namedSword = namedStack(Items.DIAMOND_SWORD, "Named");
        ItemStack plainSword = new ItemStack(Items.DIAMOND_SWORD);

        // Both should match base and middle (no filters)
        assertStackMatchesCategory(namedSword, "base");
        assertStackMatchesCategory(namedSword, "middle");
        assertStackMatchesCategory(plainSword, "base");
        assertStackMatchesCategory(plainSword, "middle");

        // Only named should match top (has filter)
        assertStackMatchesCategory(namedSword, "top");
        assertStackDoesNotMatchCategory(plainSword, "top");
    }

    // ========== Error Cases ==========

    @Test
    void unknownFilterKeySkipsCategory() {
        // Unknown filter keys should cause the category to be skipped (not loaded)
        String yaml = """
            bad_filter:
              items:
              - minecraft:diamond_sword
              filters:
              - unknown_filter: "value"
            """;

        int loaded = loadCategories(yaml);

        // Category should NOT be loaded due to invalid filter
        assertEquals(0, loaded, "Category with unknown filter should not be loaded");
        assertNull(CategoryLoader.getCategories().get("bad_filter"),
                "Category with unknown filter should not exist");
    }

    @Test
    void emptyCustomNameValueSkipsCategory() {
        // Empty filter value should cause the category to be skipped
        String yaml = """
            bad_filter:
              items:
              - minecraft:diamond_sword
              filters:
              - custom_name: ""
            """;

        int loaded = loadCategories(yaml);

        // Category should NOT be loaded due to empty filter value
        assertEquals(0, loaded, "Category with empty filter value should not be loaded");
    }

    @Test
    void validCategoryLoadedDespiteInvalidSibling() {
        // One invalid category should not prevent loading other valid categories
        String yaml = """
            valid_category:
              items:
              - minecraft:diamond_sword

            invalid_category:
              items:
              - minecraft:iron_sword
              filters:
              - unknown_filter: "bad"
            """;

        int loaded = loadCategories(yaml);

        // Only the valid category should be loaded
        assertEquals(1, loaded, "Only valid categories should be loaded");
        assertNotNull(CategoryLoader.getCategories().get("valid_category"),
                "Valid category should exist");
        assertNull(CategoryLoader.getCategories().get("invalid_category"),
                "Invalid category should not exist");
    }

    // ========== Helper method tests ==========

    @Test
    void namedStackHelperCreatesCorrectStack() {
        // Test that the namedStack helper creates stacks correctly
        ItemStack stack = namedStack(Items.DIAMOND_SWORD, "TestName");

        assertEquals(Items.DIAMOND_SWORD, stack.getItem());
        assertEquals(1, stack.getCount());
        Component customName = stack.get(DataComponents.CUSTOM_NAME);
        assertNotNull(customName);
        assertEquals("TestName", customName.getString());
    }

    @Test
    void namedStackWithCountCreatesCorrectStack() {
        // Test the count variant
        ItemStack stack = namedStack(Items.COBBLESTONE, 32, "SpecialCobble");

        assertEquals(Items.COBBLESTONE, stack.getItem());
        assertEquals(32, stack.getCount());
        Component customName = stack.get(DataComponents.CUSTOM_NAME);
        assertNotNull(customName);
        assertEquals("SpecialCobble", customName.getString());
    }

    @Test
    void filterTestWithNamedStackHelper() {
        // Verify namedStack works with filter matching
        String yaml = """
            named_swords:
              items:
              - minecraft:diamond_sword
              filters:
              - custom_name: "*"
            """;
        loadCategories(yaml);

        ItemStack named = namedStack(Items.DIAMOND_SWORD, "Excalibur");
        ItemStack plain = new ItemStack(Items.DIAMOND_SWORD);

        assertStackMatchesCategory(named, "named_swords");
        assertStackDoesNotMatchCategory(plain, "named_swords");
    }
}

