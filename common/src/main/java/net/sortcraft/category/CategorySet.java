package net.sortcraft.category;

import net.minecraft.world.item.ItemStack;
import net.sortcraft.compat.Id;

import java.util.*;

/**
 * Unified category lookup abstraction that wraps a categories map and item-to-category index.
 *
 * <p>Both global (local file) categories and share-config (downloaded) categories
 * are represented as a {@code CategorySet}. The sorting engine works with this
 * abstraction exclusively, eliminating any bifurcation between category sources.
 *
 * @see CategoryLoader#getGlobalCategorySet()
 * @see ShareConfigManager
 */
public class CategorySet {
    private final Map<String, CategoryNode> categories;
    private final Map<Id, Set<CategoryNode>> itemCategoryMap;

    /**
     * Creates a new CategorySet wrapping the provided maps.
     * The maps are defensively copied to ensure immutability.
     *
     * @param categories     Map of category name to CategoryNode
     * @param itemCategoryMap Map of item Id to set of matching CategoryNodes
     */
    public CategorySet(Map<String, CategoryNode> categories, Map<Id, Set<CategoryNode>> itemCategoryMap) {
        this.categories = Collections.unmodifiableMap(new HashMap<>(categories));

        // Deep defensive copy: wrap each inner set to ensure full immutability
        Map<Id, Set<CategoryNode>> deepCopy = new HashMap<>();
        for (Map.Entry<Id, Set<CategoryNode>> entry : itemCategoryMap.entrySet()) {
            deepCopy.put(entry.getKey(), Collections.unmodifiableSet(new HashSet<>(entry.getValue())));
        }
        this.itemCategoryMap = Collections.unmodifiableMap(deepCopy);
    }

    /**
     * Returns all categories in this set, keyed by category name.
     *
     * @return Unmodifiable view of the categories map
     */
    public Map<String, CategoryNode> getCategories() {
        return categories;
    }

    /**
     * Returns the item-to-category mapping.
     *
     * @return Unmodifiable view of the map from item Id to set of matching CategoryNodes
     */
    public Map<Id, Set<CategoryNode>> getItemCategoryMap() {
        return itemCategoryMap;
    }

    /**
     * Gets categories that match an item ID, without checking filters.
     * Returns categories sorted by priority (lower priority first).
     *
     * @param itemId The item's Id
     * @return List of matching categories sorted by priority, or empty list if none match
     * @see #getMatchingCategories(ItemStack) for filter-aware matching
     */
    public List<CategoryNode> getMatchingCategoriesNoFilter(Id itemId) {
        Set<CategoryNode> categoriesRaw = itemCategoryMap.get(itemId);
        if (categoriesRaw == null) return new ArrayList<>();
        List<CategoryNode> result = new ArrayList<>(categoriesRaw);
        Collections.sort(result);
        return result;
    }

    /**
     * Gets categories that match an item stack, including filter evaluation.
     * First finds categories by item ID, then filters them based on each
     * category's filter rules. Returns categories sorted by priority.
     *
     * @param stack The item stack to match (must not be null or empty)
     * @return List of matching categories sorted by priority, or empty list if none match
     */
    public List<CategoryNode> getMatchingCategories(ItemStack stack) {
        Id itemId = Id.ofItem(stack.getItem());
        List<CategoryNode> filteredCategories = new ArrayList<>();
        List<CategoryNode> matchedCategories = getMatchingCategoriesNoFilter(itemId);

        for (CategoryNode category : matchedCategories) {
            if (category.filters.stream().allMatch(f -> f.matches(stack))) filteredCategories.add(category);
        }

        return filteredCategories;
    }
}

