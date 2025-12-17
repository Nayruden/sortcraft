package net.sortcraft.test;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.sortcraft.category.CategoryLoader;
import net.sortcraft.category.CategoryNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Base class for Sortcraft bootstrap tests.
 * Provides common setup/teardown and helper methods for category testing.
 *
 * Bootstrap tests run with Minecraft's registries initialized in-memory,
 * allowing tests with real ItemStack objects without a full server.
 */
public abstract class SortcraftBootstrapTestBase {

    /**
     * Initialize Minecraft registries once before all tests.
     * This is a one-time cost (~3-5 seconds) that enables ItemStack creation.
     */
    @BeforeAll
    static void initMinecraft() {
        // Disable mixin application to avoid Architectury compatibility issues
        System.setProperty("mixin.env.disableRefMap", "true");
        System.setProperty("fabric.skipMixinApplication", "true");

        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    /**
     * Clear any loaded categories before each test to ensure isolation.
     */
    @BeforeEach
    void setUp() {
        CategoryLoader.clear();
    }

    /**
     * Clean up after each test.
     */
    @AfterEach
    void tearDown() {
        CategoryLoader.clear();
    }

    // ========== ItemStack Factory Helpers ==========

    /**
     * Creates an ItemStack with a custom display name.
     *
     * @param item       The item type
     * @param customName The custom name to apply
     * @return An ItemStack with the specified custom name
     */
    protected ItemStack namedStack(Item item, String customName) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(customName));
        return stack;
    }

    /**
     * Creates an ItemStack with a custom name and count.
     *
     * @param item       The item type
     * @param count      The stack count
     * @param customName The custom name to apply
     * @return An ItemStack with the specified count and custom name
     */
    protected ItemStack namedStack(Item item, int count, String customName) {
        ItemStack stack = new ItemStack(item, count);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(customName));
        return stack;
    }

    // ========== Category Loading Helpers ==========

    /**
     * Loads categories from YAML string and flattens them.
     * @return number of categories loaded
     */
    protected int loadCategories(String yaml) {
        int count = CategoryLoader.loadCategoriesFromYaml(yaml);
        CategoryLoader.flattenCategories();
        return count;
    }

    /**
     * Creates a simple category YAML with just item patterns.
     */
    protected String categoryYaml(String name, String... itemPatterns) {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(":\n");
        sb.append("  items:\n");
        for (String pattern : itemPatterns) {
            sb.append("  - ").append(pattern).append("\n");
        }
        return sb.toString();
    }

    /**
     * Creates a category YAML with priority and items.
     */
    protected String categoryYamlWithPriority(String name, int priority, String... itemPatterns) {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(":\n");
        sb.append("  priority: ").append(priority).append("\n");
        sb.append("  items:\n");
        for (String pattern : itemPatterns) {
            sb.append("  - ").append(pattern).append("\n");
        }
        return sb.toString();
    }

    /**
     * Creates a category YAML that includes other categories.
     */
    protected String categoryYamlWithIncludes(String name, int priority, String... includes) {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(":\n");
        sb.append("  priority: ").append(priority).append("\n");
        sb.append("  includes:\n");
        for (String inc : includes) {
            sb.append("  - ").append(inc).append("\n");
        }
        return sb.toString();
    }

    // ========== Category Matching Helpers ==========

    /**
     * Gets matching categories for an item.
     */
    protected List<CategoryNode> getMatchingCategories(Item item) {
        return CategoryLoader.getMatchingCategories(new ItemStack(item));
    }

    /**
     * Gets matching categories for an ItemStack.
     */
    protected List<CategoryNode> getMatchingCategories(ItemStack stack) {
        return CategoryLoader.getMatchingCategories(stack);
    }

    /**
     * Gets category names as a list of strings.
     */
    protected List<String> getCategoryNames(Item item) {
        return getMatchingCategories(item).stream()
                .map(cat -> cat.name)
                .collect(Collectors.toList());
    }

    /**
     * Gets the ResourceLocation for an item.
     */
    protected ResourceLocation getItemId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }

    // ========== Assertion Helpers ==========

    /**
     * Asserts that an item matches a specific category.
     */
    protected void assertMatchesCategory(Item item, String categoryName) {
        List<CategoryNode> matches = getMatchingCategories(item);
        assertTrue(
            matches.stream().anyMatch(c -> c.name.equals(categoryName)),
            "Expected " + item + " to match category '" + categoryName + "' but got: " +
                    matches.stream().map(c -> c.name).collect(Collectors.joining(", "))
        );
    }

    /**
     * Asserts that an item does NOT match a specific category.
     */
    protected void assertDoesNotMatchCategory(Item item, String categoryName) {
        List<CategoryNode> matches = getMatchingCategories(item);
        assertFalse(
            matches.stream().anyMatch(c -> c.name.equals(categoryName)),
            "Expected " + item + " to NOT match category '" + categoryName + "'"
        );
    }

    /**
     * Asserts that an item matches exactly the given categories in order.
     */
    protected void assertMatchesCategoriesInOrder(Item item, String... expectedCategories) {
        List<String> actual = getCategoryNames(item);
        assertEquals(expectedCategories.length, actual.size(),
            "Expected " + expectedCategories.length + " categories but got " + actual.size() + ": " + actual);
        for (int i = 0; i < expectedCategories.length; i++) {
            assertEquals(expectedCategories[i], actual.get(i),
                "Category mismatch at index " + i + ": expected '" + expectedCategories[i] +
                        "' but got '" + actual.get(i) + "'");
        }
    }

    /**
     * Asserts that an item matches no categories.
     */
    protected void assertMatchesNoCategories(Item item) {
        List<CategoryNode> matches = getMatchingCategories(item);
        assertTrue(matches.isEmpty(),
            "Expected " + item + " to match no categories but got: " +
                    matches.stream().map(c -> c.name).collect(Collectors.joining(", "))
        );
    }

    /**
     * Asserts that an item matches exactly N categories.
     */
    protected void assertMatchesCategoryCount(Item item, int expectedCount) {
        List<CategoryNode> matches = getMatchingCategories(item);
        assertEquals(expectedCount, matches.size(),
            "Expected " + item + " to match " + expectedCount + " categories but got " +
                    matches.size() + ": " + matches.stream().map(c -> c.name).collect(Collectors.joining(", "))
        );
    }

    /**
     * Asserts that a category exists and has the specified priority.
     */
    protected void assertCategoryPriority(String categoryName, int expectedPriority) {
        CategoryNode node = CategoryLoader.getCategories().get(categoryName);
        assertNotNull(node, "Category '" + categoryName + "' not found");
        assertEquals(expectedPriority, node.priority,
            "Expected priority " + expectedPriority + " for category '" + categoryName +
                    "' but got " + node.priority);
    }

    /**
     * Asserts that a category includes another category.
     */
    protected void assertCategoryIncludes(String categoryName, String includedCategory) {
        CategoryNode node = CategoryLoader.getCategories().get(categoryName);
        assertNotNull(node, "Category '" + categoryName + "' not found");
        assertTrue(node.includes.contains(includedCategory),
            "Expected category '" + categoryName + "' to include '" + includedCategory + "'");
    }

    /**
     * Asserts that an ItemStack matches a category (for filter testing).
     */
    protected void assertStackMatchesCategory(ItemStack stack, String categoryName) {
        List<CategoryNode> matches = getMatchingCategories(stack);
        assertTrue(
            matches.stream().anyMatch(c -> c.name.equals(categoryName)),
            "Expected stack to match category '" + categoryName + "' but got: " +
                    matches.stream().map(c -> c.name).collect(Collectors.joining(", "))
        );
    }

    /**
     * Asserts that an ItemStack does NOT match a category (for filter testing).
     */
    protected void assertStackDoesNotMatchCategory(ItemStack stack, String categoryName) {
        List<CategoryNode> matches = getMatchingCategories(stack);
        assertFalse(
            matches.stream().anyMatch(c -> c.name.equals(categoryName)),
            "Expected stack to NOT match category '" + categoryName + "'"
        );
    }

    /**
     * Asserts that an ItemStack matches categories in the specified order.
     */
    protected void assertStackMatchesCategoriesInOrder(ItemStack stack, String... expectedCategories) {
        List<CategoryNode> matches = getMatchingCategories(stack);
        List<String> actual = matches.stream().map(c -> c.name).collect(Collectors.toList());

        assertEquals(expectedCategories.length, actual.size(),
            "Expected " + expectedCategories.length + " categories but got " + actual.size() + ": " + actual);

        for (int i = 0; i < expectedCategories.length; i++) {
            assertEquals(expectedCategories[i], actual.get(i),
                "Category mismatch at index " + i + ": expected '" + expectedCategories[i] +
                        "' but got '" + actual.get(i) + "'");
        }
    }
}

