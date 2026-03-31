package net.sortcraft.test;

import net.minecraft.world.item.Items;
import net.sortcraft.category.CategoryLoader;
import net.sortcraft.category.CategoryNode;
import net.sortcraft.category.CategorySet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bootstrap tests for {@link CategoryLoader#loadIsolatedFromYaml(String)}.
 * Verifies that isolated category loading creates independent CategorySets
 * without affecting global state.
 */
public class IsolatedCategoryLoadingTest extends SortCraftBootstrapTestBase {

    @Test
    void loadIsolatedFromValidYaml() {
        CategorySet set = CategoryLoader.loadIsolatedFromYaml("""
            swords:
              items:
                - minecraft:diamond_sword
                - minecraft:iron_sword
            """);
        assertNotNull(set);
        assertEquals(1, set.getCategories().size());
        assertTrue(set.getCategories().containsKey("swords"));
    }

    @Test
    void loadIsolatedDoesNotAffectGlobalState() {
        // Load global categories
        loadCategories("""
            cobblestone:
              items:
                - minecraft:cobblestone
            """);

        CategorySet globalBefore = CategoryLoader.getGlobalCategorySet();
        assertNotNull(globalBefore);
        assertEquals(1, globalBefore.getCategories().size());

        // Load isolated — should NOT change global
        CategorySet isolated = CategoryLoader.loadIsolatedFromYaml("""
            swords:
              items:
                - minecraft:diamond_sword
            pickaxes:
              items:
                - minecraft:diamond_pickaxe
            """);
        assertNotNull(isolated);
        assertEquals(2, isolated.getCategories().size());

        // Global should still be the same
        CategorySet globalAfter = CategoryLoader.getGlobalCategorySet();
        assertEquals(1, globalAfter.getCategories().size());
        assertTrue(globalAfter.getCategories().containsKey("cobblestone"));
    }

    @Test
    void loadIsolatedResolvesIncludes() {
        CategorySet set = CategoryLoader.loadIsolatedFromYaml("""
            swords:
              items:
                - minecraft:diamond_sword
            weapons:
              includes:
                - swords
            """);
        assertNotNull(set);

        // Weapons should include swords' items via flattening
        Identifier swordId = BuiltInRegistries.ITEM.getKey(Items.DIAMOND_SWORD);
        List<CategoryNode> matches = set.getMatchingCategoriesNoFilter(swordId);
        assertTrue(matches.size() >= 2, "Expected diamond_sword to match both swords and weapons");
    }

    @Test
    void loadIsolatedHandlesEmptyYaml() {
        CategorySet set = CategoryLoader.loadIsolatedFromYaml("");
        assertNull(set, "Empty YAML should return null");
    }

    @Test
    void loadIsolatedHandlesMalformedYaml() {
        // SnakeYAML may throw or return unexpected data — either way global should be unaffected
        try {
            CategoryLoader.loadIsolatedFromYaml("not: valid: yaml: {{{{");
        } catch (Exception ignored) {
            // Expected — malformed YAML
        }
        // Global should be null since we haven't loaded anything
        assertNull(CategoryLoader.getGlobalCategorySet());
    }

    @Test
    void loadIsolatedHandlesZeroCategoriesYaml() {
        // Valid YAML with a category that has priority but no items —
        // still counts as a valid category, but the item-category map should be empty
        CategorySet set = CategoryLoader.loadIsolatedFromYaml("""
            empty_cat:
              priority: 5
            """);
        assertNotNull(set, "Category with priority but no items is still loaded");
        assertTrue(set.getCategories().containsKey("empty_cat"));
        assertTrue(set.getItemCategoryMap().isEmpty(),
                "Item-category map should be empty when no items are declared");
    }

    @Test
    void loadIsolatedCategorySetMatchesItems() {
        CategorySet set = CategoryLoader.loadIsolatedFromYaml("""
            gems:
              items:
                - minecraft:diamond
                - minecraft:emerald
            """);
        assertNotNull(set);

        assertFalse(set.getMatchingCategoriesNoFilter(BuiltInRegistries.ITEM.getKey(Items.DIAMOND)).isEmpty());
        assertFalse(set.getMatchingCategoriesNoFilter(BuiltInRegistries.ITEM.getKey(Items.EMERALD)).isEmpty());
        assertTrue(set.getMatchingCategoriesNoFilter(BuiltInRegistries.ITEM.getKey(Items.IRON_INGOT)).isEmpty());
    }

    @Test
    void multipleIsolatedLoadsAreIndependent() {
        CategorySet set1 = CategoryLoader.loadIsolatedFromYaml("""
            swords:
              items:
                - minecraft:diamond_sword
            """);
        CategorySet set2 = CategoryLoader.loadIsolatedFromYaml("""
            pickaxes:
              items:
                - minecraft:diamond_pickaxe
            """);

        assertNotNull(set1);
        assertNotNull(set2);

        Identifier swordId = BuiltInRegistries.ITEM.getKey(Items.DIAMOND_SWORD);
        Identifier pickaxeId = BuiltInRegistries.ITEM.getKey(Items.DIAMOND_PICKAXE);

        // set1 should only know about swords
        assertFalse(set1.getMatchingCategoriesNoFilter(swordId).isEmpty());
        assertTrue(set1.getMatchingCategoriesNoFilter(pickaxeId).isEmpty());

        // set2 should only know about pickaxes
        assertTrue(set2.getMatchingCategoriesNoFilter(swordId).isEmpty());
        assertFalse(set2.getMatchingCategoriesNoFilter(pickaxeId).isEmpty());
    }
}

