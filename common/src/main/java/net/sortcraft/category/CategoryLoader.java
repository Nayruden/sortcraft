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
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Loads and manages category configurations from YAML files.
 */
public final class CategoryLoader {
    private CategoryLoader() {}

    private static final Logger LOGGER = LoggerFactory.getLogger("sortcraft");

    private static final Map<String, CategoryNode> categories = new HashMap<>();
    private static final Map<ResourceLocation, Set<CategoryNode>> itemCategoryMap = new HashMap<>();
    private static RegistryAccess currentRegistries;

    public static Map<String, CategoryNode> getCategories() {
        return categories;
    }

    public static Map<ResourceLocation, Set<CategoryNode>> getItemCategoryMap() {
        return itemCategoryMap;
    }

    public static RegistryAccess getCurrentRegistries() {
        return currentRegistries;
    }

    public static void setCurrentRegistries(RegistryAccess registries) {
        currentRegistries = registries;
    }

    /**
     * Clears all loaded categories. Called on server stop.
     */
    public static void clear() {
        categories.clear();
        itemCategoryMap.clear();
        currentRegistries = null;
    }

    /**
     * Loads categories from a YAML string. Intended for testing purposes.
     *
     * @param yamlContent the YAML content to parse
     * @return the number of categories loaded
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

    @SuppressWarnings("unchecked")
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
                categoryNode.includes.addAll((List<String>) includeCategories);
            } else if (includeRaw != null) {
                LOGGER.warn("Category '{}' has unrecognized includes type {}", categoryName, includeRaw.getClass().getName());
            }

            Object filtersRaw = categoryConf.get("filters");
            if (filtersRaw instanceof List<?> filters) {
                for (Object filterRaw : filters) {
                    if (filterRaw instanceof Map<?, ?> filterMap) {
                        var filter = (Map<String, String>) filterMap;
                        for (Map.Entry<String, String> filterEntry : filter.entrySet())
                            categoryNode.filters.add(FilterRuleFactory.fromYaml(currentRegistries, filterEntry.getKey(), filterEntry.getValue()));
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
     */
    public static void flattenCategories() {
        for (String categoryName : categories.keySet()) flattenCategory(categoryName);

        for (CategoryNode category : categories.values()) {
            for (ResourceLocation itemId : category.flattenedItemIds)
                itemCategoryMap.computeIfAbsent(itemId, k -> new HashSet<CategoryNode>()).add(category);
        }
    }

    /**
     * Gets matching categories for an item ID (without filter check).
     */
    public static List<CategoryNode> getMatchingCategoriesNoFilter(ResourceLocation itemId) {
        Set<CategoryNode> categoriesRaw = itemCategoryMap.get(itemId);
        if (categoriesRaw == null) return new ArrayList<>();
        List<CategoryNode> result = new ArrayList<>(categoriesRaw);
        Collections.sort(result);
        return result;
    }

    /**
     * Gets matching categories for an item stack (with filter check).
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
        if (id != null) {
            categoryNode.itemIds.add(id);
        } else {
            LOGGER.warn("Invalid item identifier '{}' in category '{}'", itemName, categoryName);
        }
    }
}

