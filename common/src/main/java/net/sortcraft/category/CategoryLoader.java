package net.sortcraft.category;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.sortcraft.FilterRuleFactory;
import net.sortcraft.config.ConfigManager;
import net.sortcraft.compat.RegistryHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Loads and manages category configurations from YAML files.
 *
 * <p>Categories define how items are grouped for sorting. Each category can specify:
 * <ul>
 *   <li><b>items</b> - Direct item IDs, regex patterns (/pattern/), or tag references (#tag)</li>
 *   <li><b>includes</b> - References to other categories to inherit their items</li>
 *   <li><b>filters</b> - Rules to match items by properties (enchantments, names, durability)</li>
 *   <li><b>priority</b> - Order in which categories are checked (lower = first, default: 10)</li>
 * </ul>
 *
 * <p>Category files are loaded from {@code config/sortcraft/categories/} on server start.
 * Use {@link #loadCategoriesFromYaml(String)} for testing or dynamic category loading.
 *
 * @see CategoryNode
 * @see net.sortcraft.FilterRule
 */
public final class CategoryLoader {
    private CategoryLoader() {}

    private static final Logger LOGGER = LoggerFactory.getLogger("sortcraft");

    private static final Map<String, CategoryNode> categories = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, Set<CategoryNode>> itemCategoryMap = new ConcurrentHashMap<>();
    private static volatile RegistryAccess currentRegistries;

    /**
     * Returns all loaded categories, keyed by category name.
     *
     * @return Unmodifiable view of the categories map
     */
    public static Map<String, CategoryNode> getCategories() {
        return Collections.unmodifiableMap(categories);
    }

    /**
     * Returns the item-to-category mapping built by {@link #flattenCategories()}.
     * Maps each item ID to the set of categories that include it.
     *
     * @return Unmodifiable view of the map from item ResourceLocation to set of matching CategoryNodes
     */
    public static Map<ResourceLocation, Set<CategoryNode>> getItemCategoryMap() {
        return Collections.unmodifiableMap(itemCategoryMap);
    }

    /**
     * Returns the current registry access used for filter creation.
     * Set during {@link #loadCategories(MinecraftServer)}.
     *
     * @return The registry access, or null if not yet initialized
     */
    public static RegistryAccess getCurrentRegistries() {
        return currentRegistries;
    }

    /**
     * Sets the registry access for filter creation.
     * Called automatically by {@link #loadCategories(MinecraftServer)}.
     *
     * @param registries The registry access to use
     */
    public static void setCurrentRegistries(RegistryAccess registries) {
        currentRegistries = registries;
    }

    /**
     * Clears all loaded categories and resets state.
     * Called on server stop to free resources and prepare for reload.
     *
     * <p>After calling this method:
     * <ul>
     *   <li>All categories are removed</li>
     *   <li>The item-to-category map is cleared</li>
     *   <li>The registry access reference is nulled</li>
     * </ul>
     */
    public static void clear() {
        categories.clear();
        itemCategoryMap.clear();
        currentRegistries = null;
    }

    /**
     * Loads categories from a YAML string.
     * Primarily intended for testing purposes, but can be used for dynamic category loading.
     *
     * <p>The YAML content should be a map of category names to category definitions:
     * <pre>{@code
     * my_category:
     *   items:
     *   - minecraft:diamond
     *   priority: 5
     * }</pre>
     *
     * @param yamlContent the YAML content to parse
     * @return the number of categories successfully loaded
     * @see #loadCategories(MinecraftServer) for loading from files
     */
    @SuppressWarnings("unchecked")
    public static int loadCategoriesFromYaml(String yamlContent) {
        Yaml yaml = new Yaml();
        Object data = yaml.load(yamlContent);
        if (data == null || !(data instanceof Map)) {
            LOGGER.warn("YAML content does not contain a valid map");
            return 0;
        }

        int count = loadCategoriesFromMap((Map<String, Object>) data, "YAML string");
        LOGGER.info("Loaded {} categories from YAML string", count);
        return count;
    }

    /**
     * Shared method to load categories from a parsed YAML map.
     *
     * @param mapRoot the parsed YAML map containing category definitions
     * @param sourceName descriptive name of the source (for logging)
     * @return the number of categories loaded
     */
    private static int loadCategoriesFromMap(Map<String, Object> mapRoot, String sourceName) {
        int count = 0;
        for (Map.Entry<String, Object> entry : mapRoot.entrySet()) {
            String categoryName = entry.getKey();
            Object valueRaw = entry.getValue();
            if (!(valueRaw instanceof Map)) {
                LOGGER.warn("Category '{}' in {} has a non-map value, skipping", categoryName, sourceName);
                continue;
            }
            if (categories.containsKey(categoryName)) {
                LOGGER.warn("Duplicate category '{}' in {}, overwriting previous definition", categoryName, sourceName);
            }
            CategoryNode categoryNode = parseCategory(categoryName, valueRaw);
            if (categoryNode != null) {
                categories.put(categoryName, categoryNode);
                count++;
            }
        }
        return count;
    }

    /**
     * Loads categories from all YAML files in the categories directory.
     *
     * <p>Scans {@code config/sortcraft/categories/} for all {@code .yaml} and {@code .yml} files,
     * loading them in alphabetical order. If the directory doesn't exist, creates it with an
     * example category file.
     *
     * <p>This method also sets the current registry access from the server, which is required
     * for filters that reference enchantments or other registry objects.
     *
     * @param server The Minecraft server to get registry access from
     * @see #flattenCategories() Must be called after loading to build the item-to-category map
     */
    @SuppressWarnings("unchecked")
    public static void loadCategories(MinecraftServer server) {
        currentRegistries = server.registryAccess();
        Path categoriesDir = ConfigManager.getConfigPath("categories");
        try {
            if (!Files.exists(categoriesDir)) {
                Files.createDirectories(categoriesDir);
                String example = "# Example categories configuration\n"
                        + "example_category:\n"
                        + "  items:\n"
                        + "  - minecraft:iron_ingot\n"
                        + "  - minecraft:gold_ingot\n";
                Files.write(categoriesDir.resolve("example.yaml"), example.getBytes(StandardCharsets.UTF_8));
                LOGGER.warn("Categories directory not found, created example at {}", categoriesDir);
            }

            Yaml yaml = new Yaml();
            List<Path> yamlFiles;
            try (Stream<Path> paths = Files.list(categoriesDir)) {
                yamlFiles = paths
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".yaml") || p.toString().endsWith(".yml"))
                        .sorted()
                        .collect(Collectors.toList());
            }

            int filesLoaded = 0;
            for (Path yamlFile : yamlFiles) {
                try (InputStream in = Files.newInputStream(yamlFile)) {
                    Object data = yaml.load(in);
                    if (data == null) {
                        LOGGER.debug("Skipping empty/comments-only file: {}", yamlFile.getFileName());
                        continue;
                    }
                    if (!(data instanceof Map)) {
                        LOGGER.warn("File '{}' does not contain a map at root level, skipping", yamlFile.getFileName());
                        continue;
                    }
                    loadCategoriesFromMap((Map<String, Object>) data, yamlFile.getFileName().toString());
                    filesLoaded++;
                } catch (Exception err) {
                    LOGGER.error("Error loading categories from file: {}", yamlFile.getFileName(), err);
                }
            }
            LOGGER.info("Loaded {} categories from {} files in {}", categories.size(), filesLoaded, categoriesDir);
        } catch (IOException err) {
            LOGGER.error("IO error while loading categories directory", err);
        } catch (Exception err) {
            LOGGER.error("Fatal error while loading categories directory", err);
        }
    }

    private static CategoryNode parseCategory(String categoryName, Object valueRaw) {
        if (categoryName == null || categoryName.isEmpty()) {
            LOGGER.warn("Cannot parse category with null or empty name");
            return null;
        }
        if (valueRaw == null) {
            LOGGER.warn("Cannot parse category '{}', value is null", categoryName);
            return null;
        }
        try {
            CategoryNode categoryNode = new CategoryNode(categoryName);

            if (!(valueRaw instanceof Map<?, ?> categoryConf)) {
                LOGGER.warn("Cannot parse category '{}', not a dictionary type", valueRaw);
                return null;
            }

            Object itemPatternsValue = categoryConf.get("items");
            if (itemPatternsValue instanceof List<?> itemPatterns) {
                for (var entry : itemPatterns) {
                    if (entry instanceof String pattern) {
                        expandItemPattern(pattern, categoryName, categoryNode);
                    } else {
                        LOGGER.warn("Unsupported item entry type '{}' in category '{}'",
                                entry.getClass().getSimpleName(), categoryName);
                    }
                }
            } else if (itemPatternsValue != null) {
                LOGGER.warn("Category '{}' has unrecognized 'items' type '{}'", categoryName, itemPatternsValue.getClass().getName());
            }

            Object includeRaw = categoryConf.get("includes");
            if (includeRaw instanceof List<?> includeCategories) {
                for (Object item : includeCategories) {
                    if (item instanceof String s) {
                        categoryNode.includes.add(s);
                    } else if (item != null) {
                        LOGGER.warn("Category '{}' has non-string include value: {}", categoryName, item);
                    }
                }
            } else if (includeRaw != null) {
                LOGGER.warn("Category '{}' has unrecognized includes type {}", categoryName, includeRaw.getClass().getName());
            }

            Object filtersRaw = categoryConf.get("filters");
            if (filtersRaw instanceof List<?> filters) {
                for (Object filterRaw : filters) {
                    if (filterRaw instanceof Map<?, ?> filterMap) {
                        for (Map.Entry<?, ?> entry : filterMap.entrySet()) {
                            String key = String.valueOf(entry.getKey());
                            String value = entry.getValue() != null ? String.valueOf(entry.getValue()) : null;
                            categoryNode.filters.add(FilterRuleFactory.fromYaml(currentRegistries, key, value));
                        }
                    }
                }
            } else if (filtersRaw != null) {
                LOGGER.warn("Category '{}' has unrecognized filter type {}", categoryName, filtersRaw.getClass().getName());
            }

            Object priorityRaw = categoryConf.get("priority");
            if (priorityRaw instanceof Integer priority) {
                categoryNode.priority = (int) priority;
            } else if (priorityRaw != null) {
                LOGGER.warn("Category '{}' has unrecognized priority type {}", categoryName, priorityRaw.getClass().getName());
            }

            return categoryNode;
        } catch (IllegalArgumentException err) {
            LOGGER.warn("Invalid configuration for category '{}': {}", categoryName, err.getMessage());
            return null;
        } catch (ClassCastException err) {
            LOGGER.warn("Type mismatch in category '{}' configuration: {}", categoryName, err.getMessage());
            return null;
        } catch (Exception err) {
            LOGGER.warn("Failed to parse category '{}'", categoryName, err);
            return null;
        }
    }

    private static Set<ResourceLocation> flattenCategory(String categoryName) {
        return flattenCategory(categoryName, new HashSet<>());
    }

    private static Set<ResourceLocation> flattenCategory(String categoryName, Set<String> visitedCategories) {
        CategoryNode category = categories.get(categoryName);
        if (category == null) throw new IllegalArgumentException("Unknown category: " + categoryName);

        Set<ResourceLocation> items = category.flattenedItemIds;
        if (items != null) return items;

        if (visitedCategories.contains(categoryName)) throw new IllegalStateException("Cycle detected: " + categoryName);
        visitedCategories.add(categoryName);

        items = new HashSet<>();
        category.flattenedItemIds = items;

        items.addAll(category.itemIds);
        for (String childName : category.includes) {
            if (!categories.containsKey(childName)) {
                LOGGER.warn("Category '{}' includes unknown category '{}', skipping", categoryName, childName);
                continue;
            }
            items.addAll(flattenCategory(childName, visitedCategories));
        }

        return items;
    }

    /**
     * Flattens all categories and builds the item-to-category map.
     *
     * <p>This method resolves all {@code includes} references, expanding each category
     * to contain all items from its included categories (recursively). It also builds
     * the reverse mapping from item IDs to categories for efficient lookup.
     *
     * <p>Must be called after {@link #loadCategories(MinecraftServer)} and before
     * any sorting operations.
     *
     * @throws IllegalStateException if a circular include reference is detected
     */
    public static void flattenCategories() {
        for (String categoryName : categories.keySet()) flattenCategory(categoryName);

        for (CategoryNode category : categories.values()) {
            for (ResourceLocation itemId : category.flattenedItemIds)
                itemCategoryMap.computeIfAbsent(itemId, k -> new HashSet<CategoryNode>()).add(category);
        }
    }

    /**
     * Gets categories that match an item ID, without checking filters.
     *
     * <p>Returns categories sorted by priority (lower priority first).
     * This method is useful for diagnostics or when you need to see all
     * potential categories before filter evaluation.
     *
     * @param itemId The item's ResourceLocation (e.g., "minecraft:diamond_sword")
     * @return List of matching categories sorted by priority, or empty list if none match
     * @see #getMatchingCategories(ItemStack) for filter-aware matching
     */
    public static List<CategoryNode> getMatchingCategoriesNoFilter(ResourceLocation itemId) {
        Set<CategoryNode> categoriesRaw = itemCategoryMap.get(itemId);
        if (categoriesRaw == null) return new ArrayList<>();
        List<CategoryNode> result = new ArrayList<>(categoriesRaw);
        Collections.sort(result);
        return result;
    }

    /**
     * Gets categories that match an item stack, including filter evaluation.
     *
     * <p>First finds categories by item ID, then filters them based on each
     * category's filter rules (enchantments, custom names, durability, etc.).
     * Returns categories sorted by priority (lower priority first).
     *
     * <p>This is the primary method used by the sorting engine to determine
     * where an item should be sorted.
     *
     * @param stack The item stack to match (must not be null or empty)
     * @return List of matching categories sorted by priority, or empty list if none match
     */
    public static List<CategoryNode> getMatchingCategories(ItemStack stack) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        List<CategoryNode> filteredCategories = new ArrayList<>();
        List<CategoryNode> matchedCategories = getMatchingCategoriesNoFilter(itemId);

        for (CategoryNode category : matchedCategories) {
            if (category.filters.stream().allMatch(f -> f.matches(stack))) filteredCategories.add(category);
        }

        return filteredCategories;
    }

    // ========== Item Pattern Expansion Helpers ==========

    /**
     * Expands a pattern string into item IDs and adds them to the category.
     * Supports: explicit item IDs, regex patterns (/regex/), and tag references (#tag).
     */
    private static void expandItemPattern(String pattern, String categoryName, CategoryNode categoryNode) {
        if (isRegexPattern(pattern)) {
            expandRegexPattern(pattern, categoryNode);
        } else if (pattern.startsWith("#")) {
            expandTag(pattern, categoryName, categoryNode);
        } else {
            addExplicitItem(pattern, categoryName, categoryNode);
        }
    }

    private static boolean isRegexPattern(String s) {
        return s.length() > 1 && s.charAt(0) == '/' && s.charAt(s.length() - 1) == '/';
    }

    private static void expandRegexPattern(String patternStr, CategoryNode categoryNode) {
        String regex = patternStr.substring(1, patternStr.length() - 1);
        Pattern pattern = Pattern.compile(regex);
        BuiltInRegistries.ITEM.keySet().stream()
                .filter(itemId -> pattern.matcher(itemId.toString()).find())
                .forEach(categoryNode.itemIds::add);
    }

    private static void expandTag(String tagRef, String categoryName, CategoryNode categoryNode) {
        String tagId = tagRef.substring(1);
        ResourceLocation tagLocation = ResourceLocation.tryParse(tagId);
        if (tagLocation == null) {
            LOGGER.warn("Invalid tag reference '{}' in category '{}'", tagRef, categoryName);
            return;
        }

        TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagLocation);
        List<ResourceLocation> matchedItems = BuiltInRegistries.ITEM.keySet().stream()
                .filter(itemId -> {
                    Item item = RegistryHelper.getItemByKey(itemId);
                    return item != null && BuiltInRegistries.ITEM.wrapAsHolder(item).is(tagKey);
                })
                .toList();

        if (matchedItems.isEmpty()) {
            LOGGER.warn("Tag '{}' matched no items for category '{}'", tagRef, categoryName);
        } else {
            categoryNode.itemIds.addAll(matchedItems);
            LOGGER.debug("Expanded tag '{}' to {} items in category '{}'", tagRef, matchedItems.size(), categoryName);
        }
    }

    private static void addExplicitItem(String itemName, String categoryName, CategoryNode categoryNode) {
        ResourceLocation id = ResourceLocation.tryParse(itemName);
        if (id == null) {
            LOGGER.warn("Invalid item identifier '{}' in category '{}'", itemName, categoryName);
            return;
        }
        if (!BuiltInRegistries.ITEM.containsKey(id)) {
            LOGGER.warn("Unknown item '{}' in category '{}' - item does not exist in registry", itemName, categoryName);
        }
        categoryNode.itemIds.add(id);
    }
}

