package net.sortcraft.category;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.sortcraft.FilterRule;
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
    private static MinecraftServer currentServer;

    public static Map<String, CategoryNode> getCategories() {
        return categories;
    }

    public static Map<ResourceLocation, Set<CategoryNode>> getItemCategoryMap() {
        return itemCategoryMap;
    }

    public static MinecraftServer getCurrentServer() {
        return currentServer;
    }

    public static void setCurrentServer(MinecraftServer server) {
        currentServer = server;
    }

    /**
     * Clears all loaded categories. Called on server stop.
     */
    public static void clear() {
        categories.clear();
        itemCategoryMap.clear();
        currentServer = null;
    }

    /**
     * Loads categories from all YAML files in the categories directory.
     */
    @SuppressWarnings("unchecked")
    public static void loadCategories(MinecraftServer server) {
        currentServer = server;
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
                    Map<String, Object> mapRoot = (Map<String, Object>) data;
                    for (Map.Entry<String, Object> entry : mapRoot.entrySet()) {
                        String categoryName = entry.getKey();
                        Object valueRaw = entry.getValue();
                        if (!(valueRaw instanceof Map)) {
                            LOGGER.warn("Category '{}' in file '{}' has a non-map value, skipping", categoryName, yamlFile.getFileName());
                            continue;
                        }
                        if (categories.containsKey(categoryName)) {
                            LOGGER.warn("Duplicate category '{}' in file '{}', overwriting previous definition", categoryName, yamlFile.getFileName());
                        }
                        CategoryNode categoryNode = parseCategory(categoryName, valueRaw);
                        if (categoryNode != null) {
                            categories.put(categoryName, categoryNode);
                        }
                    }
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

            if (!(valueRaw instanceof Map<?, ?>)) {
                LOGGER.warn("Cannot parse category '{}', not a dictionary type", valueRaw);
                return null;
            }
            Map<String, Object> categoryConf = (Map<String, Object>) valueRaw;

            boolean anyFilter = false;
            var filteredItems = new HashSet<>(BuiltInRegistries.ITEM.keySet());
            Object itemPatternsValue = categoryConf.get("items");
            if (itemPatternsValue instanceof List<?> itemPatterns) {
                for (var patternRaw : itemPatterns) {
                    switch (patternRaw) {
                        case String patternStr when (patternStr.length() > 1 && patternStr.charAt(0) == '/' && patternStr.charAt(patternStr.length() - 1) == '/') -> {
                            var pattern = Pattern.compile(patternStr.substring(1, patternStr.length() - 1));
                            BuiltInRegistries.ITEM.keySet().stream()
                                    .filter(itemId -> pattern.matcher(itemId.toString()).find())
                                    .forEach(categoryNode.itemIds::add);
                        }
                        case String itemName -> {
                            ResourceLocation id = ResourceLocation.tryParse(itemName);
                            if (id != null) {
                                categoryNode.itemIds.add(id);
                            } else {
                                LOGGER.warn("Invalid item identifier '{}' in category '{}'", itemName, categoryName);
                            }
                        }
                        case Map<?, ?> filterAttrs -> {
                            anyFilter = true;
                            List<FilterRule> filters = filterAttrs.entrySet().stream()
                                    .map((entry) -> FilterRuleFactory.fromYaml(currentServer, (String) entry.getKey(), (String) entry.getValue()))
                                    .toList();
                            HashSet<ResourceLocation> newFilteredItems = new HashSet<>();
                            for (ResourceLocation id : filteredItems) {
                                ItemStack stack = new ItemStack(RegistryHelper.getItemOrThrow(id));
                                boolean allMatch = true;
                                for (FilterRule filter : filters) {
                                    if (!filter.matches(stack)) {
                                        allMatch = false;
                                        break;
                                    }
                                }
                                if (allMatch) {
                                    newFilteredItems.add(id);
                                }
                            }
                            filteredItems = newFilteredItems;
                        }
                        default -> LOGGER.warn("Unhandled item pattern type '{}' in category '{}'", patternRaw, categoryName);
                    }
                }
            } else if (itemPatternsValue != null) {
                LOGGER.warn("Category '{}' has unrecognized 'items' type '{}'", categoryName, itemPatternsValue.getClass().getName());
            }

            if (anyFilter && !filteredItems.isEmpty()) {
                categoryNode.itemIds.addAll(filteredItems);
            } else if (anyFilter) {
                LOGGER.warn("Item patterns in category '{}' filtered out all items!", categoryName);
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
                            categoryNode.filters.add(FilterRuleFactory.fromYaml(currentServer, filterEntry.getKey(), filterEntry.getValue()));
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
        for (String childName : category.includes) items.addAll(flattenCategory(childName, visitedCategories));

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
}

