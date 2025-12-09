package net.sortcraft.test;

import net.minecraft.world.item.Items;
import net.sortcraft.category.CategoryLoader;
import net.sortcraft.category.CategoryNode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bootstrap tests for category parsing edge cases.
 * Tests YAML parsing, includes resolution, priority handling, and error cases.
 */
public class CategoryParsingTest extends SortcraftBootstrapTestBase {

    // ========== Basic Parsing ==========

    @Test
    void parseSingleCategory() {
        String yaml = categoryYaml("swords", "minecraft:diamond_sword", "minecraft:iron_sword");
        int count = loadCategories(yaml);

        assertEquals(1, count, "Should load 1 category");
        assertMatchesCategory(Items.DIAMOND_SWORD, "swords");
        assertMatchesCategory(Items.IRON_SWORD, "swords");
    }

    @Test
    void parseMultipleCategories() {
        String yaml = categoryYaml("swords", "minecraft:diamond_sword") +
                      categoryYaml("pickaxes", "minecraft:diamond_pickaxe");
        int count = loadCategories(yaml);

        assertEquals(2, count, "Should load 2 categories");
        assertMatchesCategory(Items.DIAMOND_SWORD, "swords");
        assertMatchesCategory(Items.DIAMOND_PICKAXE, "pickaxes");
    }

    // ========== Priority Handling ==========

    @Test
    void priorityOrderingLowerFirst() {
        String yaml = categoryYamlWithPriority("low", 10, "minecraft:diamond_sword") +
                      categoryYamlWithPriority("high", 100, "minecraft:diamond_sword");
        loadCategories(yaml);

        // Lower priority values come first (ascending order)
        assertMatchesCategoriesInOrder(Items.DIAMOND_SWORD, "low", "high");
    }

    @Test
    void defaultPriorityIsTen() {
        String yaml = categoryYaml("default", "minecraft:diamond_sword") +
                      categoryYamlWithPriority("explicit", 10, "minecraft:iron_sword");
        loadCategories(yaml);

        assertCategoryPriority("default", 10);
        assertCategoryPriority("explicit", 10);
    }

    @Test
    void negativePriorityAllowed() {
        String yaml = categoryYamlWithPriority("negative", -10, "minecraft:diamond_sword") +
                      categoryYamlWithPriority("zero", 0, "minecraft:diamond_sword");
        loadCategories(yaml);

        // Lower priority values come first (-10 before 0)
        assertMatchesCategoriesInOrder(Items.DIAMOND_SWORD, "negative", "zero");
    }

    // ========== Includes Resolution ==========

    @Test
    void includesResolvesItems() {
        String yaml = categoryYaml("base", "minecraft:diamond_sword") +
                      categoryYamlWithIncludes("extended", 0, "base");
        loadCategories(yaml);

        // Extended should match diamond_sword through include
        assertMatchesCategory(Items.DIAMOND_SWORD, "extended");
    }

    @Test
    void multipleIncludes() {
        String yaml = categoryYaml("swords", "minecraft:diamond_sword") +
                      categoryYaml("pickaxes", "minecraft:diamond_pickaxe") +
                      "tools:\n  includes:\n  - swords\n  - pickaxes\n";
        loadCategories(yaml);

        assertMatchesCategory(Items.DIAMOND_SWORD, "tools");
        assertMatchesCategory(Items.DIAMOND_PICKAXE, "tools");
    }

    @Test
    void nestedIncludes() {
        String yaml = categoryYaml("base", "minecraft:diamond_sword") +
                      categoryYamlWithIncludes("mid", 0, "base") +
                      categoryYamlWithIncludes("top", 0, "mid");
        loadCategories(yaml);

        // Top should match through mid -> base
        assertMatchesCategory(Items.DIAMOND_SWORD, "top");
    }

    // ========== Regex Patterns ==========

    @Test
    void regexPatternMatches() {
        // Regex patterns must be wrapped in /pattern/ syntax
        String yaml = """
            swords:
              items:
              - "/minecraft:.*_sword/"
            """;
        loadCategories(yaml);

        assertMatchesCategory(Items.DIAMOND_SWORD, "swords");
        assertMatchesCategory(Items.IRON_SWORD, "swords");
        assertMatchesCategory(Items.WOODEN_SWORD, "swords");
        assertDoesNotMatchCategory(Items.DIAMOND_PICKAXE, "swords");
    }

    @Test
    void wildcardPattern() {
        // Regex patterns must be wrapped in /pattern/ syntax
        String yaml = """
            everything:
              items:
              - "/minecraft:.*/"
            """;
        loadCategories(yaml);

        assertMatchesCategory(Items.DIAMOND_SWORD, "everything");
        assertMatchesCategory(Items.COBBLESTONE, "everything");
    }

    // ========== Edge Cases ==========

    @Test
    void emptyItemsList() {
        String yaml = """
            empty:
              items: []
            """;
        loadCategories(yaml);

        assertMatchesNoCategories(Items.DIAMOND_SWORD);
    }

    @Test
    void categoryWithOnlyIncludes() {
        String yaml = categoryYaml("base", "minecraft:diamond_sword") +
                      "wrapper:\n  includes:\n  - base\n";
        loadCategories(yaml);

        assertMatchesCategory(Items.DIAMOND_SWORD, "wrapper");
    }

    @Test
    void duplicateItemInMultipleCategories() {
        String yaml = categoryYamlWithPriority("cat1", 10, "minecraft:diamond_sword") +
                      categoryYamlWithPriority("cat2", 5, "minecraft:diamond_sword");
        loadCategories(yaml);

        // Should match both categories
        assertMatchesCategoryCount(Items.DIAMOND_SWORD, 2);
    }

    @Test
    void nonExistentItemPattern() {
        String yaml = categoryYaml("fake", "minecraft:nonexistent_item");
        loadCategories(yaml);

        // Should load without error, just won't match anything
        Map<String, CategoryNode> categories = CategoryLoader.getCategories();
        assertTrue(categories.containsKey("fake"));
    }

    @Test
    void missingIncludeIgnored() {
        // Include a category that doesn't exist
        String yaml = "broken:\n  includes:\n  - nonexistent\n";

        // Should not throw, just log warning
        assertDoesNotThrow(() -> loadCategories(yaml));
    }

    @Test
    void selfIncludeHandled() {
        // Category includes itself - should not cause infinite loop
        String yaml = """
            recursive:
              items:
              - minecraft:diamond_sword
              includes:
              - recursive
            """;

        assertDoesNotThrow(() -> loadCategories(yaml));
        assertMatchesCategory(Items.DIAMOND_SWORD, "recursive");
    }

    @Test
    void circularIncludesHandled() {
        // A includes B, B includes A
        String yaml = """
            catA:
              items:
              - minecraft:diamond_sword
              includes:
              - catB
            catB:
              items:
              - minecraft:iron_sword
              includes:
              - catA
            """;

        assertDoesNotThrow(() -> loadCategories(yaml));
        // Both should match both items through circular includes
        assertMatchesCategory(Items.DIAMOND_SWORD, "catA");
        assertMatchesCategory(Items.DIAMOND_SWORD, "catB");
    }

    // ========== YAML Format Variations ==========

    @Test
    void yamlWithComments() {
        String yaml = """
            # This is a comment
            swords:
              # Another comment
              items:
              - minecraft:diamond_sword  # inline comment
            """;
        loadCategories(yaml);

        assertMatchesCategory(Items.DIAMOND_SWORD, "swords");
    }

    @Test
    void yamlWithExtraWhitespace() {
        String yaml = """
            swords:
              items:
                - minecraft:diamond_sword
                - minecraft:iron_sword
            """;
        loadCategories(yaml);

        assertMatchesCategory(Items.DIAMOND_SWORD, "swords");
        assertMatchesCategory(Items.IRON_SWORD, "swords");
    }

    @Test
    void categoryNameWithSpecialChars() {
        String yaml = """
            my-category_name:
              items:
              - minecraft:diamond_sword
            """;
        loadCategories(yaml);

        assertMatchesCategory(Items.DIAMOND_SWORD, "my-category_name");
    }
}

