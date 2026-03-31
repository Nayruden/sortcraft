package net.sortcraft.test;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.sortcraft.category.CategoryLoader;
import net.sortcraft.category.CategoryNode;
import net.sortcraft.category.CategorySet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bootstrap tests for the {@link CategorySet} abstraction.
 * Tests matching, priority ordering, immutability, and defensive copying.
 */
public class CategorySetTest extends SortCraftBootstrapTestBase {

    /** Helper to create a simple CategoryNode with flattenedItemIds set. */
    private CategoryNode createCategory(String name, int priority, Identifier... itemIds) {
        CategoryNode node = new CategoryNode(name);
        node.priority = priority;
        node.flattenedItemIds = new HashSet<>(Arrays.asList(itemIds));
        return node;
    }

    /** Build item-to-category map from categories. */
    private Map<Identifier, Set<CategoryNode>> buildItemMap(CategoryNode... categories) {
        Map<Identifier, Set<CategoryNode>> map = new HashMap<>();
        for (CategoryNode cat : categories) {
            if (cat.flattenedItemIds != null) {
                for (Identifier id : cat.flattenedItemIds) {
                    map.computeIfAbsent(id, k -> new HashSet<>()).add(cat);
                }
            }
        }
        return map;
    }

    @Test
    void matchingCategoriesReturnsSortedByPriority() {
        Identifier swordId = BuiltInRegistries.ITEM.getKey(Items.DIAMOND_SWORD);
        CategoryNode highPriority = createCategory("high_priority", 1, swordId);
        CategoryNode lowPriority = createCategory("low_priority", 20, swordId);

        CategorySet set = new CategorySet(
                Map.of("high_priority", highPriority, "low_priority", lowPriority),
                buildItemMap(highPriority, lowPriority)
        );

        List<CategoryNode> matches = set.getMatchingCategories(new ItemStack(Items.DIAMOND_SWORD));
        assertEquals(2, matches.size());
        assertEquals("high_priority", matches.get(0).name);
        assertEquals("low_priority", matches.get(1).name);
    }

    @Test
    void matchingCategoriesNoFilterSkipsFilters() {
        // Load categories with a custom_name filter via YAML
        loadCategories("""
            filtered_swords:
              filters:
              - custom_name: '*'
              items:
                - minecraft:diamond_sword
            """);

        CategorySet globalSet = CategoryLoader.getGlobalCategorySet();
        assertNotNull(globalSet);

        Identifier swordId = BuiltInRegistries.ITEM.getKey(Items.DIAMOND_SWORD);

        // NoFilter should return the category even though the stack has no custom name
        List<CategoryNode> noFilterMatches = globalSet.getMatchingCategoriesNoFilter(swordId);
        assertEquals(1, noFilterMatches.size());
        assertEquals("filtered_swords", noFilterMatches.get(0).name);

        // getMatchingCategories should filter it out (no custom name on plain stack)
        List<CategoryNode> filteredMatches = globalSet.getMatchingCategories(new ItemStack(Items.DIAMOND_SWORD));
        assertTrue(filteredMatches.isEmpty());
    }

    @Test
    void noMatchReturnsEmptyList() {
        CategorySet emptySet = new CategorySet(Map.of(), Map.of());

        List<CategoryNode> noFilterResult = emptySet.getMatchingCategoriesNoFilter(BuiltInRegistries.ITEM.getKey(Items.DIAMOND_SWORD));
        assertNotNull(noFilterResult);
        assertTrue(noFilterResult.isEmpty());

        List<CategoryNode> filterResult = emptySet.getMatchingCategories(new ItemStack(Items.DIAMOND_SWORD));
        assertNotNull(filterResult);
        assertTrue(filterResult.isEmpty());
    }

    @Test
    void categorySetIsImmutable() {
        Identifier swordId = BuiltInRegistries.ITEM.getKey(Items.DIAMOND_SWORD);
        CategoryNode cat = createCategory("swords", 10, swordId);

        CategorySet set = new CategorySet(
                Map.of("swords", cat),
                buildItemMap(cat)
        );

        assertThrows(UnsupportedOperationException.class, () ->
                set.getCategories().put("new_cat", new CategoryNode("new_cat")));
        assertThrows(UnsupportedOperationException.class, () ->
                set.getItemCategoryMap().put(BuiltInRegistries.ITEM.getKey(Items.IRON_SWORD), new HashSet<>()));
    }

    @Test
    void defensiveCopyPreventsExternalMutation() {
        Identifier swordId = BuiltInRegistries.ITEM.getKey(Items.DIAMOND_SWORD);
        CategoryNode cat = createCategory("swords", 10, swordId);

        Map<String, CategoryNode> catMap = new HashMap<>();
        catMap.put("swords", cat);
        Map<Identifier, Set<CategoryNode>> itemMap = new HashMap<>();
        itemMap.put(swordId, new HashSet<>(Set.of(cat)));

        CategorySet set = new CategorySet(catMap, itemMap);

        // Mutate the original maps
        catMap.put("injected", new CategoryNode("injected"));
        itemMap.put(BuiltInRegistries.ITEM.getKey(Items.IRON_SWORD), new HashSet<>());

        // CategorySet should not be affected
        assertFalse(set.getCategories().containsKey("injected"));
        assertFalse(set.getItemCategoryMap().containsKey(BuiltInRegistries.ITEM.getKey(Items.IRON_SWORD)));
        assertEquals(1, set.getCategories().size());
        assertEquals(1, set.getItemCategoryMap().size());
    }
}

