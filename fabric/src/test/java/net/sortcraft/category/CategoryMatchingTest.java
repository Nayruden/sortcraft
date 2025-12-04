package net.sortcraft.category;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.sortcraft.test.SortCraftBootstrapTestBase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bootstrap unit tests for category matching logic.
 *
 * These tests use Fabric Loader JUnit to initialize Minecraft's registries
 * in-memory, allowing us to test with real ItemStack objects without
 * requiring a full Minecraft server.
 *
 * Extends SortCraftBootstrapTestBase which handles Minecraft bootstrap
 * and category cleanup between tests.
 */
class CategoryMatchingTest extends SortCraftBootstrapTestBase {

    /**
     * Test that a diamond sword matches the "swords" category when using
     * a regex pattern that matches all items ending with "_sword".
     */
    @Test
    void diamondSwordMatchesSwordsCategory() {
        // Given: A category configuration that matches all swords via regex
        String yamlConfig = """
            swords:
              items:
              - /.*:.*_sword/
            """;

        // When: We load the categories and flatten them
        int loaded = CategoryLoader.loadCategoriesFromYaml(yamlConfig);
        CategoryLoader.flattenCategories();

        // Then: Exactly one category was loaded
        assertEquals(1, loaded, "Should load exactly one category");

        // And: A diamond sword ItemStack should match the "swords" category
        ItemStack diamondSword = new ItemStack(Items.DIAMOND_SWORD);
        List<CategoryNode> matchingCategories = CategoryLoader.getMatchingCategories(diamondSword);

        assertFalse(matchingCategories.isEmpty(), "Diamond sword should match at least one category");
        assertEquals(1, matchingCategories.size(), "Diamond sword should match exactly one category");
        assertEquals("swords", matchingCategories.get(0).name, "Diamond sword should match 'swords' category");
    }

    /**
     * Test that a diamond sword matches multiple overlapping categories
     * and they are returned in priority order.
     */
    @Test
    void diamondSwordMatchesMultipleCategoriesByPriority() {
        // Given: Multiple categories with different priorities
        String yamlConfig = """
            swords:
              priority: 10
              items:
              - /.*:.*_sword/

            weapons:
              priority: 15
              includes:
              - swords

            gear:
              priority: 20
              includes:
              - weapons
            """;

        // When: We load and flatten categories
        CategoryLoader.loadCategoriesFromYaml(yamlConfig);
        CategoryLoader.flattenCategories();

        // Then: Diamond sword matches all three categories in priority order
        ItemStack diamondSword = new ItemStack(Items.DIAMOND_SWORD);
        List<CategoryNode> matchingCategories = CategoryLoader.getMatchingCategories(diamondSword);

        assertEquals(3, matchingCategories.size(), "Diamond sword should match 3 categories");
        assertEquals("swords", matchingCategories.get(0).name, "First match should be 'swords' (priority 10)");
        assertEquals("weapons", matchingCategories.get(1).name, "Second match should be 'weapons' (priority 15)");
        assertEquals("gear", matchingCategories.get(2).name, "Third match should be 'gear' (priority 20)");
    }

    /**
     * Test that explicit item IDs work correctly.
     */
    @Test
    void explicitItemIdMatches() {
        // Given: A category with an explicit item ID
        String yamlConfig = """
            special_swords:
              items:
              - minecraft:diamond_sword
              - minecraft:netherite_sword
            """;

        // When: We load and flatten categories
        CategoryLoader.loadCategoriesFromYaml(yamlConfig);
        CategoryLoader.flattenCategories();

        // Then: Diamond sword matches, but iron sword does not
        ItemStack diamondSword = new ItemStack(Items.DIAMOND_SWORD);
        ItemStack ironSword = new ItemStack(Items.IRON_SWORD);

        List<CategoryNode> diamondMatches = CategoryLoader.getMatchingCategories(diamondSword);
        List<CategoryNode> ironMatches = CategoryLoader.getMatchingCategories(ironSword);

        assertEquals(1, diamondMatches.size(), "Diamond sword should match");
        assertEquals("special_swords", diamondMatches.get(0).name);

        assertTrue(ironMatches.isEmpty(), "Iron sword should NOT match special_swords");
    }

    /**
     * Test that items not matching any category return an empty list.
     */
    @Test
    void nonMatchingItemReturnsEmpty() {
        // Given: A category that only matches swords
        String yamlConfig = """
            swords:
              items:
              - /.*:.*_sword/
            """;

        CategoryLoader.loadCategoriesFromYaml(yamlConfig);
        CategoryLoader.flattenCategories();

        // When: We check a pickaxe
        ItemStack diamondPickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        List<CategoryNode> matches = CategoryLoader.getMatchingCategories(diamondPickaxe);

        // Then: No categories match
        assertTrue(matches.isEmpty(), "Diamond pickaxe should not match 'swords' category");
    }

    /**
     * Test that the item-to-category map is built correctly after flattening.
     */
    @Test
    void itemCategoryMapContainsDiamondSword() {
        // Given: Categories are loaded and flattened
        String yamlConfig = """
            swords:
              items:
              - /.*:.*_sword/
            """;

        CategoryLoader.loadCategoriesFromYaml(yamlConfig);
        CategoryLoader.flattenCategories();

        // When: We look up the diamond sword's resource location
        ResourceLocation diamondSwordId = BuiltInRegistries.ITEM.getKey(Items.DIAMOND_SWORD);

        // Then: The item-to-category map contains the diamond sword
        assertTrue(
            CategoryLoader.getItemCategoryMap().containsKey(diamondSwordId),
            "Item category map should contain diamond_sword"
        );
    }
}

