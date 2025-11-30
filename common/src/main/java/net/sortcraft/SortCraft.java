package net.sortcraft;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.sortcraft.compat.EventHelper;
import net.sortcraft.compat.RegistryHelper;
import net.sortcraft.platform.Platform;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.core.config.Configurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SortCraft {
    public static final String MODID = "sortcraft";
    private static final Logger LOGGER = LoggerFactory.getLogger(MODID);
    private static final Map<String, CategoryNode> categories = new HashMap<>();
    private static final Map<ResourceLocation, Set<CategoryNode>> itemCategoryMap = new HashMap<>();

    // Constants
    private static final String SIGN_PREFIX = "[";
    private static final String SIGN_SUFFIX = "]";
    private static final String INPUT_SIGN_TEXT = formatSignText("input");
    private static final int INPUT_SIGN_SEARCH_RADIUS = 20;
    private static final int UNIFORM_CONTAINER_THRESHOLD = 10;
    private static final int PARTICLE_DURATION_TICKS = 10 * 20;
    private static final int SHULKER_BOX_SIZE = 27;
    private static final int CHEST_SLOT_COUNT = 27;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Shared scheduler for particle effects - lazy initialized
    private static ScheduledExecutorService particleScheduler;

    protected static MinecraftServer currentServer;

    // Configuration values
    private static int searchRadius = 64;

    // Helper method for sign text formatting
    private static String formatSignText(String text) {
        return SIGN_PREFIX + text + SIGN_SUFFIX;
    }

    // Helper method for config path resolution
    private static Path getSortCraftConfigPath(String... subPaths) {
        Path base = Platform.getConfigDir().resolve("sortcraft");
        for (String subPath : subPaths) {
            base = base.resolve(subPath);
        }
        return base;
    }

    // Helper method for getting chest container - reduces code duplication
    private static Optional<Container> getChestContainer(ServerLevel world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock chestBlock)) return Optional.empty();
        return Optional.ofNullable(ChestBlock.getContainer(chestBlock, state, world, pos, true));
    }

    // Helper methods for registry access - delegates to version-specific RegistryHelper
    private static Item getItemOrThrow(ResourceLocation id) {
        return RegistryHelper.getItemOrThrow(id);
    }

    private static Item getItemByKey(ResourceLocation id) {
        return RegistryHelper.getItemByKey(id);
    }

    // Get or create the shared particle scheduler
    private static synchronized ScheduledExecutorService getParticleScheduler() {
        if (particleScheduler == null || particleScheduler.isShutdown()) {
            particleScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "SortCraft-Particles");
                t.setDaemon(true);
                return t;
            });
        }
        return particleScheduler;
    }

    public static void init() {
        LifecycleEvent.SERVER_STARTING.register(server -> {
            loadConfig();
            loadCategoriesConfig(server);
            flattenCategories();
        });

        CommandRegistrationEvent.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerSortCommand(dispatcher);
        });

        InteractionEvent.RIGHT_CLICK_BLOCK.register((player, hand, pos, direction) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return EventHelper.pass();
            if (hand != InteractionHand.MAIN_HAND) return EventHelper.pass();

            ServerLevel world = (ServerLevel) serverPlayer.level();
            BlockState state = world.getBlockState(pos);
            if (!(state.getBlock() instanceof WallSignBlock)) return EventHelper.pass();

            BlockEntity be = world.getBlockEntity(pos);
            if (!(be instanceof SignBlockEntity signBe)) return EventHelper.pass();

            if (findTextOnSign(signBe, INPUT_SIGN_TEXT) != null) {
                CommandSourceStack source = serverPlayer.createCommandSourceStack();
                try {
                    executeSortInput(source, false);
                } catch (Exception e) {
                    LOGGER.error("Error executing sort from sign click", e);
                }
                return EventHelper.success();
            }

            return EventHelper.pass();
        });
    }

    @SuppressWarnings("unchecked")
    private static void loadConfig() {
        Path configPath = getSortCraftConfigPath("config.yaml");
        org.apache.logging.log4j.Level logLevel = org.apache.logging.log4j.Level.WARN;
        try {
            if (!Files.exists(configPath)) {
                Files.createDirectories(configPath.getParent());
                String defaultConfig = """
                        # SortCraft Configuration

                        # Log level: TRACE, DEBUG, INFO, WARN, ERROR
                        # Default: WARN
                        logLevel: WARN

                        # Search radius for finding signs (in blocks)
                        # Default: 64
                        searchRadius: 64
                        """;
                Files.write(configPath, defaultConfig.getBytes(StandardCharsets.UTF_8));
                LOGGER.info("Created default config.yaml at {}", configPath);
            }

            Yaml yaml = new Yaml();
            try (InputStream in = Files.newInputStream(configPath)) {
                Map<String, Object> config = yaml.load(in);
                if (config != null) {
                    Object logLevelValue = config.get("logLevel");
                    if (logLevelValue instanceof String logLevelStr) {
                        try {
                            logLevel = org.apache.logging.log4j.Level.valueOf(logLevelStr.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            LOGGER.warn("Invalid logLevel '{}' in config.yaml, using WARN", logLevelStr);
                        }
                    }

                    Object searchRadiusValue = config.get("searchRadius");
                    if (searchRadiusValue instanceof Integer radius) {
                        searchRadius = radius;
                    } else if (searchRadiusValue instanceof Number radius) {
                        searchRadius = radius.intValue();
                    }
                }
            }

            // Configure Log4j2 logger level
            Configurator.setLevel(MODID, logLevel);
            LOGGER.info("Loaded config: logLevel={}, searchRadius={}", logLevel, searchRadius);
        } catch (IOException e) {
            LOGGER.error("Error loading config.yaml", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void loadCategoriesConfig(MinecraftServer server) {
        currentServer = server;
        Path categoriesDir = getSortCraftConfigPath("categories");
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
                        // Empty file or comments-only file, skip silently
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
        try {
            CategoryNode categoryNode = new CategoryNode();
            categoryNode.name = categoryName;

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
                            // Filter items more efficiently: reuse ItemStack per item, avoid stream for allMatch
                            HashSet<ResourceLocation> newFilteredItems = new HashSet<>();
                            for (ResourceLocation id : filteredItems) {
                                ItemStack stack = new ItemStack(getItemOrThrow(id));
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
                categoryNode.includes = new HashSet<>((List<String>) includeCategories);
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
        } catch (Exception err) {
            LOGGER.warn(String.format("Failed to parse category '%s'", categoryName), err);
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

    private static void flattenCategories() {
        for (String categoryName : categories.keySet()) flattenCategory(categoryName);

        for (CategoryNode category : categories.values()) {
            for (ResourceLocation itemId : category.flattenedItemIds)
                itemCategoryMap.computeIfAbsent(itemId, k -> new HashSet<CategoryNode>()).add(category);
        }
    }

    private static void registerSortCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("sort")
                        .then(Commands.literal("input")
                                .executes(ctx -> executeSortInput(ctx.getSource(), false)))
                        .then(Commands.literal("preview")
                                .executes(ctx -> executeSortInput(ctx.getSource(), true)))
                        .then(Commands.literal("diagnostics")
                                .executes(SortCraft::executeSortDiag))
                        .then(Commands.literal("whereis")
                                .then(Commands.argument("item", StringArgumentType.greedyString())
                                        .suggests((context, builder) ->
                                                SharedSuggestionProvider.suggestResource(BuiltInRegistries.ITEM.keySet(), builder))
                                        .executes(ctx -> executeSortWhereIs(ctx))))
                        .then(Commands.literal("category")
                                .then(Commands.argument("item", StringArgumentType.greedyString())
                                        .suggests((context, builder) ->
                                                SharedSuggestionProvider.suggestResource(BuiltInRegistries.ITEM.keySet(), builder))
                                        .executes(ctx -> executeSortCategory(ctx))))
                        .then(Commands.literal("help")
                                .executes(SortCraft::executeSortHelp))
                        .then(Commands.literal("dump")
                                .executes(SortCraft::dumpItemTags))
                        .then(Commands.literal("reload")
                                .executes(SortCraft::executeSortReload))
        );
    }

    private static int dumpItemTags(CommandContext<CommandSourceStack> context) {
        try {
            Map<String, List<String>> itemsToTags = new HashMap<>();
            Map<String, List<String>> tagsToItems = new HashMap<>();

            for (ResourceLocation id : BuiltInRegistries.ITEM.keySet()) {
                Item item = getItemByKey(id);
                Holder<Item> entry = BuiltInRegistries.ITEM.wrapAsHolder(item);

                Collection<TagKey<Item>> tags = entry.tags().toList();
                List<String> tagList = new ArrayList<>();

                for (TagKey<Item> tag : tags) {
                    String tagStr = tag.location().toString();
                    tagList.add(tagStr);
                    tagsToItems.computeIfAbsent(tagStr, k -> new ArrayList<>()).add(id.toString());
                }

                itemsToTags.put(id.toString(), tagList);
            }

            Path itemsToTagsPath = getSortCraftConfigPath("items_to_tags.json");
            try (FileWriter writer = new FileWriter(itemsToTagsPath.toFile())) {
                writer.write(GSON.toJson(itemsToTags));
            }

            Path tagsToItemsPath = getSortCraftConfigPath("tags_to_items.json");
            try (FileWriter writer = new FileWriter(tagsToItemsPath.toFile())) {
                writer.write(GSON.toJson(tagsToItems));
            }

            context.getSource().sendSuccess(() -> Component.literal("Dumped items_to_tags.json and tags_to_items.json"), false);
        } catch (Exception e) {
            LOGGER.error("Error dumping item tags", e);
            context.getSource().sendFailure(Component.literal("Error dumping item tags: " + e.getMessage()));
        }
        return 1;
    }

    private static int executeSortHelp(CommandContext<CommandSourceStack> context) {
        String helpMessage = String.format("""
                Sort Command Help:
                /sort input           - Sorts items from the closest input chest (chest must have a sign with '%s')
                /sort preview         - Shows a preview of what will be sorted and where
                /sort diagnostics     - Generates a diagnostics report as YAML
                /sort whereis <item>  - Finds chests that contain the specified item
                /sort category <item> - Shows the sorting category for the specified item
                /sort reload          - Reloads category configurations from config files
                /sort dump            - Generates JSON files with all item tags from the registry
                /sort help            - Shows this help message

                All commands support autocomplete. Use TAB for suggestions.
                """, INPUT_SIGN_TEXT);
        context.getSource().sendSuccess(() -> Component.literal(helpMessage), false);
        return 1;
    }

    private static int executeSortReload(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();

        try {
            loadConfig();
            categories.clear();
            itemCategoryMap.clear();
            loadCategoriesConfig(server);
            flattenCategories();

            source.sendSuccess(() -> Component.literal("SortCraft configuration reloaded successfully."), false);
            LOGGER.info("[sortreload] Configuration reloaded successfully.");
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error reloading configuration: " + e.getMessage()));
            LOGGER.error("[sortreload] Failed to reload configuration.", e);
            return 0;
        }
    }

    private static String findTextOnSign(SignBlockEntity sign, String text) {
        return findTextOnSign(sign, text, false);
    }

    private static String findTextOnSign(SignBlockEntity sign, String text, boolean isRegex) {
        // Normalize text to lowercase for non-regex comparisons to ensure consistent matching
        String normalizedText = isRegex ? text : text.toLowerCase();

        Component[] lines = sign.getFrontText().getMessages(false);
        for (Component line : lines) {
            if (line == null) continue;

            String lineText = line.getString().trim().toLowerCase();
            if (!isRegex) {
                if (lineText.contains(normalizedText)) return lineText;
            } else {
                if (lineText.matches(normalizedText)) return lineText;
            }
        }
        return null;
    }

    /**
     * Finds the closest sign with the given text using the provided context cache.
     * @param context The sort context containing the sign cache
     * @param text The text to search for on signs
     * @return The closest sign with matching text, or null if not found
     */
    private static SignBlockEntity findClosestSignWithText(SortContext context, String text) {
        SignBlockEntity sign = context.findSign(text);
        if (sign != null) {
            LOGGER.debug("[findClosestSignWithText] Found cached sign at {} for text '{}'", sign.getBlockPos(), text);
        }
        return sign;
    }

    static List<CategoryNode> getMatchingCategoriesNoFilter(ResourceLocation itemId) {
        Set<CategoryNode> categoriesRaw = itemCategoryMap.get(itemId);
        if (categoriesRaw == null) return new ArrayList<>();
        List<CategoryNode> categories = new ArrayList<>(categoriesRaw);
        Collections.sort(categories);
        return categories;
    }

    static List<CategoryNode> getMatchingCategories(ItemStack stack) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        List<CategoryNode> filteredCategories = new ArrayList<>();
        // getMatchingCategoriesNoFilter already returns a sorted list, and filtering preserves order
        List<CategoryNode> categories = getMatchingCategoriesNoFilter(itemId);

        for (CategoryNode category : categories) {
            if (category.filters.stream().allMatch(f -> f.matches(stack))) filteredCategories.add(category);
        }

        // No need to sort again - the filtered list maintains the sorted order from getMatchingCategoriesNoFilter
        return filteredCategories;
    }

    private static ResourceLocation getSingleItemIfUniformAndMeetsThreshold(Iterable<ItemStack> stacks, int threshold) {
        ResourceLocation singleItem = null;
        int count = 0;

        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());

            if (singleItem == null) {
                singleItem = itemId;
            } else if (!singleItem.equals(itemId)) {
                return null;
            }
            count++;
        }

        if (count >= threshold) {
            return singleItem;
        }
        return null;
    }

    private static SortingResults sortStacks(SortContext context, ServerLevel world, Iterable<ItemStack> stacks, boolean preview) {
        SortingResults sortingResults = new SortingResults();
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;

            LOGGER.debug("[sortinput] Sorting {} of {}", stack.getCount(), stack.getItem().toString());

            Iterable<ItemStack> innerStacks = getStacksIfContainer(stack);
            if (innerStacks != null) {
                ResourceLocation uniformItem = getSingleItemIfUniformAndMeetsThreshold(innerStacks, UNIFORM_CONTAINER_THRESHOLD);
                if (uniformItem != null) {
                    LOGGER.debug("[sortinput] Container has >={} stacks of same item '{}'. Sorting container itself into that category.",
                            UNIFORM_CONTAINER_THRESHOLD, uniformItem);

                    List<CategoryNode> cats = getMatchingCategoriesNoFilter(uniformItem);
                    sortStack(context, world, preview, stack, cats, uniformItem, sortingResults);
                    continue;
                }

                LOGGER.debug("[sortinput] Item is a container. Sorting contents of container.");
                SortingResults innerSortingResults = sortStacks(context, world, innerStacks, preview);
                sortingResults.sorted += innerSortingResults.sorted;
                sortingResults.overflowCategories.addAll(innerSortingResults.overflowCategories);
                sortingResults.unknownItems.addAll(innerSortingResults.unknownItems);
                innerSortingResults.categoryCounts.forEach((category, cnt) ->
                        sortingResults.categoryCounts.merge(category, cnt, Integer::sum)
                );

                if (!preview) {
                    if (isBundle(stack)) {
                        BundleContents bundleContents;
                        if (innerSortingResults.leftovers.isEmpty()) bundleContents = new BundleContents(List.of());
                        else bundleContents = new BundleContents(innerSortingResults.leftovers);
                        stack.set(DataComponents.BUNDLE_CONTENTS, bundleContents);
                    } else {
                        NonNullList<ItemStack> restored = NonNullList.withSize(SHULKER_BOX_SIZE, ItemStack.EMPTY);
                        for (int i = 0; i < innerSortingResults.leftovers.size() && i < SHULKER_BOX_SIZE; i++) {
                            restored.set(i, innerSortingResults.leftovers.get(i));
                        }
                        stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(restored));
                    }
                }

                if (!(innerSortingResults.leftovers.isEmpty())) {
                    LOGGER.debug("[sortinput] Inner container not completely emptied, giving up further sorting.");
                    sortingResults.leftovers.add(stack);
                    continue;
                }
            }

            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            List<CategoryNode> cats = getMatchingCategories(stack);
            sortStack(context, world, preview, stack, cats, itemId, sortingResults);
        }
        return sortingResults;
    }

    private static void sortStack(SortContext context, ServerLevel world, boolean preview, ItemStack stack,
                                  List<CategoryNode> cats, ResourceLocation itemId, SortingResults sortingResults) {
        if (cats.isEmpty()) {
            LOGGER.debug("[sortinput] No categories found for item: {}", itemId);
            sortingResults.unknownItems.add(itemId.toString());
            sortingResults.leftovers.add(stack);
            return;
        }

        int stackSize = stack.getCount();
        int totalMoved = 0;
        String categoriesStr = CategoryNode.categoriesToStr(cats);
        for (CategoryNode category : cats) {
            List<ChestRef> categoryChests = findCategoryChests(context, world, category.name);
            if (categoryChests.isEmpty()) continue;

            int moved = distributeToChests(stack, categoryChests, preview);
            totalMoved += moved;
            if (moved > 0) {
                sortingResults.sorted += moved;
                Map<String, Integer> counts = sortingResults.categoryCounts;
                counts.put(category.name, counts.getOrDefault(category.name, 0) + moved);
                LOGGER.debug("[sortinput] Moved {} of item {}", moved, itemId);

                if (preview && totalMoved >= stackSize) break;
            }
        }

        if (totalMoved < stackSize) {
            LOGGER.debug("[sortinput] Overflow: Could not store (all of) item '{}' -> categories '{}'", itemId, categoriesStr);
            sortingResults.overflowCategories.add(cats.getFirst().name);
            sortingResults.leftovers.add(stack);
        }
    }

    private static int executeSortInput(CommandSourceStack source, boolean preview) throws CommandSyntaxException {
        ServerLevel world = source.getLevel();
        BlockPos playerPos = source.getPlayer().blockPosition();
        LOGGER.debug("[sortinput] Starting sort near {}", playerPos);

        // Create a per-command context that caches sign positions
        // Use searchRadius for sorting signs, but INPUT_SIGN_SEARCH_RADIUS is smaller (20 blocks)
        // We use Math.max to ensure the context can find both the input sign and category signs
        SortContext context = new SortContext(world, playerPos, Math.max(searchRadius, INPUT_SIGN_SEARCH_RADIUS));

        SignBlockEntity inputSign = findClosestSignWithText(context, INPUT_SIGN_TEXT);

        if (inputSign == null) {
            source.sendSuccess(() -> Component.literal("No input sign found nearby."), false);
            LOGGER.debug("[sortinput] No input sign found within search radius.");
            return 0;
        }

        BlockPos chestPos = getAttachedChestPos(inputSign.getBlockPos(), inputSign.getBlockState(), world);
        LOGGER.debug("[sortinput] Attached chest position resolved: {}", chestPos);

        if (chestPos == null) {
            source.sendSuccess(() -> Component.literal("Input sign isn't attached to a chest."), false);
            LOGGER.debug("[sortinput] Sign at {} is not attached to a chest.", inputSign.getBlockPos());
            return 0;
        }

        BlockState state = world.getBlockState(chestPos);
        if (!(state.getBlock() instanceof ChestBlock chestBlock)) {
            source.sendSuccess(() -> Component.literal("Block attached to the input sign is not a chest."), false);
            LOGGER.debug("[sortinput] Block at {} is not a ChestBlock.", chestPos);
            return 0;
        }

        Container inputInv = ChestBlock.getContainer(chestBlock, state, world, chestPos, true);
        if (inputInv == null) {
            source.sendSuccess(() -> Component.literal("Could not access input chest inventory."), false);
            LOGGER.debug("[sortinput] Failed to access chest inventory at {}", chestPos);
            return 0;
        }

        LOGGER.debug("[sortinput] Input chest inventory loaded. Beginning sort.");

        SortingResults results = sortStacks(context, world, containerToIterable(inputInv), preview);

        StringBuilder message = new StringBuilder();
        message.append(summarize(results.overflowCategories, "⚠ Storage overflow in following categories:"));
        message.append(summarize(results.unknownItems, "⚠ No category found for following items:"));
        if (!message.isEmpty()) {
            final String messageStr = message.toString();
            source.sendSuccess(() -> Component.literal(messageStr), false);
        }

        if (preview) {
            Map<String, Integer> counts = results.categoryCounts;
            if (counts.isEmpty()) {
                source.sendSuccess(() -> Component.literal("No items to sort."), false);
            } else {
                source.sendSuccess(() -> Component.literal("Sort Preview:"), false);
                for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                    String cat = entry.getKey();
                    int count = entry.getValue();
                    source.sendSuccess(() -> Component.literal("- " + cat + ": " + count + " item" + (count != 1 ? "s" : "")), false);
                }
            }
            return 1;
        }

        if (results.sorted > 0) {
            world.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    chestPos.getX() + 0.5,
                    chestPos.getY() + 1.0,
                    chestPos.getZ() + 0.5,
                    10, 0.4, 0.5, 0.4, 0.1);
            String totalSortedStr = Integer.toString(results.sorted);
            source.sendSuccess(() -> Component.literal(totalSortedStr + " items sorted successfully."), false);
            LOGGER.debug("[sortinput] Sorting complete. {} total items sorted.", totalSortedStr);
        } else {
            source.sendSuccess(() -> Component.literal("No items were sorted."), false);
            LOGGER.debug("[sortinput] No items were sorted.");
        }

        return 1;
    }

    private static Iterable<ItemStack> containerToIterable(Container container) {
        return () -> new Iterator<>() {
            private int index = 0;
            private final int size = container.getContainerSize();

            @Override
            public boolean hasNext() {
                return index < size;
            }

            @Override
            public ItemStack next() {
                return container.getItem(index++);
            }
        };
    }

    private static int executeSortWhereIs(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerLevel world = source.getLevel();
        BlockPos playerPos = source.getPlayer().blockPosition();
        String itemName = StringArgumentType.getString(context, "item");

        ResourceLocation itemId = ResourceLocation.tryParse(itemName);
        if (itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId)) {
            source.sendFailure(Component.literal("Unknown item: " + itemName));
            return 0;
        }

        Item item = getItemByKey(itemId);

        // Build container cache once for this command
        SortContext sortContext = new SortContext(world, playerPos, searchRadius);
        sortContext.buildContainerCache();
        Map<BlockPos, Container> containerCache = sortContext.getContainerCache();

        if (containerCache.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No nearby containers found."), false);
            return 1;
        }

        // Convert cached containers to iterable stacks for searching
        Map<BlockPos, Iterable<ItemStack>> foundStorage = new HashMap<>();
        for (Map.Entry<BlockPos, Container> entry : containerCache.entrySet()) {
            foundStorage.put(entry.getKey(), containerToIterable(entry.getValue()));
        }

        List<BlockPos> validPositions = foundStorage.entrySet().stream()
                .filter(entry -> stacksContainsItem(entry.getValue(), item))
                .sorted(Comparator.comparingDouble(entry -> entry.getKey().distSqr(playerPos)))
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (validPositions.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No " + itemName + " found in nearby containers."), false);
            return 1;
        }

        source.sendSuccess(() -> Component.literal("Item '" + itemName + "' found in:"), false);

        // Use the shared particle scheduler instead of creating a new one each time
        ScheduledExecutorService scheduler = getParticleScheduler();

        for (BlockPos pos : validPositions) {
            source.sendSuccess(() -> Component.literal("- " + pos.toShortString()), false);

            double x = pos.getX() + 0.5;
            double y = pos.getY() + 1.0;
            double z = pos.getZ() + 0.5;

            for (int i = 0; i < PARTICLE_DURATION_TICKS; i += 2) {
                long delayMillis = i * 50L;
                scheduler.schedule(() -> {
                    world.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 10, 0.5, 0.5, 0.5, 0.05);
                }, delayMillis, TimeUnit.MILLISECONDS);
            }
        }

        return 1;
    }

    private static boolean stacksContainsItem(Iterable<ItemStack> stacks, Item item) {
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;
            if (stack.getItem() == item) return true;

            Iterable<ItemStack> nested = getStacksIfContainer(stack);
            if (nested != null) {
                if (stacksContainsItem(nested, item)) return true;
            }
        }
        return false;
    }

    private static int executeSortCategory(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String itemName = StringArgumentType.getString(context, "item");

        ResourceLocation id = ResourceLocation.tryParse(itemName);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            source.sendFailure(Component.literal("Unknown item: " + itemName));
            return 0;
        }
        Set<CategoryNode> cats = itemCategoryMap.get(id);
        if (!(cats == null || cats.isEmpty())) {
            String categoriesStr = CategoryNode.categoriesToStr(cats);
            source.sendSuccess(() -> Component.literal("Item " + id + " belongs to categories '" + categoriesStr + "'."), false);
        } else {
            source.sendSuccess(() -> Component.literal("Item " + id + " is not assigned to any category."), false);
        }
        return 1;
    }

    private static int executeSortDiag(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerLevel world = source.getLevel();
        BlockPos playerPos = source.getPlayer().blockPosition();
        Map<String, Map<String, Object>> categoryData = new TreeMap<>();

        // Create context for caching sign lookups during diagnostics
        SortContext sortContext = new SortContext(world, playerPos, searchRadius);

        for (Map.Entry<String, CategoryNode> entry : categories.entrySet()) {
            String categoryName = entry.getKey();
            CategoryNode categoryNode = entry.getValue();
            Set<ResourceLocation> items = categoryNode.flattenedItemIds;

            if (items == null || items.isEmpty()) continue;

            List<ChestRef> chests = findCategoryChests(sortContext, world, categoryName);
            if (chests.isEmpty()) continue;

            Map<String, Object> itemData = new TreeMap<>();
            int totalSlots = chests.size() * CHEST_SLOT_COUNT;
            int usedSlots = 0;

            for (ResourceLocation id : items) {
                Item item = getItemByKey(id);
                if (item == null) continue;

                int total = 0;
                Map<String, Integer> locationCounts = new LinkedHashMap<>();

                for (ChestRef ref : chests) {
                    Container inv = ref.inventory;
                    int count = 0;
                    for (int slot = 0; slot < inv.getContainerSize(); slot++) {
                        ItemStack stack = inv.getItem(slot);
                        if (stack.getItem() == item) {
                            count += stack.getCount();
                            if (!stack.isEmpty()) usedSlots++;
                        }
                    }
                    if (count > 0) {
                        String loc = ref.pos.getX() + " " + ref.pos.getY() + " " + ref.pos.getZ();
                        locationCounts.merge(loc, count, Integer::sum);
                        total += count;
                    }
                }

                if (total > 0) {
                    Map<String, Object> itemEntry = new LinkedHashMap<>();
                    itemEntry.put("total_quantity", total);
                    if (!locationCounts.isEmpty()) {
                        List<Map<String, Object>> chestLocations = new ArrayList<>();
                        for (Map.Entry<String, Integer> e : locationCounts.entrySet()) {
                            Map<String, Object> loc = new LinkedHashMap<>();
                            loc.put("location", e.getKey());
                            loc.put("quantity", e.getValue());
                            chestLocations.add(loc);
                        }
                        itemEntry.put("chest_locations", chestLocations);
                    }
                    itemData.put(id.toString(), itemEntry);
                }
            }

            if (!itemData.isEmpty()) {
                float spaceUsed = (totalSlots > 0) ? ((float) usedSlots / totalSlots) * 100f : 0f;
                itemData.put("space_used", String.format("%.0f%%", spaceUsed));
                categoryData.put(categoryName, itemData);
            }
        }

        Map<String, Object> finalYaml = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Object>> e : categoryData.entrySet()) {
            finalYaml.put(e.getKey(), e.getValue());
        }

        try {
            File file = getSortCraftConfigPath("sortdiag.yaml").toFile();
            file.getParentFile().mkdirs();

            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);

            Yaml yaml = new Yaml(options);
            try (FileWriter writer = new FileWriter(file)) {
                yaml.dump(finalYaml, writer);
            }
        } catch (IOException e) {
            source.sendFailure(Component.literal("Failed to write sortcraft/sortdiag.yaml: " + e.getMessage()));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Sorter diagnostic written to sortcraft/sortdiag.yaml"), false);
        return 1;
    }

    private static boolean isBundle(ItemStack stack) {
        return stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, null) != null;
    }

    private static Iterable<ItemStack> getStacksIfContainer(ItemStack stack) {
        BundleContents bundle = stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, null);
        if (bundle != null) {
            List<ItemStack> bundleItems = new ArrayList<>();
            bundle.items().forEach(bundleItems::add);
            return bundleItems;
        }

        ItemContainerContents container = stack.getOrDefault(DataComponents.CONTAINER, null);
        if (container != null) {
            NonNullList<ItemStack> containerStacks = NonNullList.withSize(SHULKER_BOX_SIZE, ItemStack.EMPTY);
            container.copyInto(containerStacks);
            return containerStacks;
        }

        return null;
    }

    private static int distributeToChests(ItemStack stack, List<ChestRef> chests, boolean preview) {
        int originalCount = stack.getCount();
        int toSort = originalCount;
        int maxStackSize = Math.min(stack.getMaxStackSize(), 64);

        for (ChestRef ref : chests) {
            Container inv = ref.inventory;

            for (int slot = 0; slot < inv.getContainerSize() && toSort > 0; slot++) {
                ItemStack target = inv.getItem(slot);
                if (!target.isEmpty() && ItemStack.isSameItemSameComponents(stack, target) && target.getCount() < target.getMaxStackSize()) {
                    int space = target.getMaxStackSize() - target.getCount();
                    int move = Math.min(space, toSort);
                    if (!preview) target.grow(move);
                    toSort -= move;
                }
            }

            for (int slot = 0; slot < inv.getContainerSize() && toSort > 0; slot++) {
                if (inv.getItem(slot).isEmpty()) {
                    int move = Math.min(toSort, maxStackSize);
                    if (!preview) {
                        ItemStack toPut = stack.copy();
                        toPut.setCount(move);
                        inv.setItem(slot, toPut);
                    }
                    toSort -= move;
                }
            }

            if (toSort == 0) break;
        }

        int moved = originalCount - toSort;
        if (!preview) stack.shrink(moved);
        return moved;
    }

    private static BlockPos getAttachedChestPos(BlockPos signPos, BlockState signState, ServerLevel world) {
        Direction attachedDirection = signState.getValue(BlockStateProperties.HORIZONTAL_FACING);

        if (attachedDirection == null) return null;
        attachedDirection = attachedDirection.getOpposite();

        BlockPos chestPos = signPos.relative(attachedDirection);
        BlockEntity be = world.getBlockEntity(chestPos);
        if (be instanceof ChestBlockEntity) {
            return chestPos;
        }
        return null;
    }

    private static List<BlockPos> getChestBlocks(BlockPos pos, ServerLevel world) {
        BlockState state = world.getBlockState(pos);
        List<BlockPos> blocks = new ArrayList<>();
        blocks.add(pos);
        ChestType chestType = state.getValue(BlockStateProperties.CHEST_TYPE);
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);

        if (chestType == null || facing == null || chestType == ChestType.SINGLE) return blocks;
        if (chestType == ChestType.RIGHT) {
            blocks.add(pos.relative(facing.getCounterClockWise()));
        } else {
            blocks.add(pos.relative(facing.getClockWise()));
        }
        return blocks;
    }

    private static List<ChestRef> collectChestStack(ServerLevel world, BlockPos startPos) {
        List<ChestRef> result = new ArrayList<>();
        BlockPos cur = startPos;

        while (true) {
            BlockState state = world.getBlockState(cur);

            Block block = state.getBlock();
            if (block instanceof ChestBlock chestBlock) {
                Container inv = ChestBlock.getContainer(chestBlock, state, world, cur, true);
                if (inv != null) {
                    result.add(new ChestRef(cur, inv));
                    LOGGER.trace("[cheststack] Added chest at {}", cur);
                }
            }

            BlockPos below = cur.below();
            if (!(world.getBlockEntity(below) instanceof ChestBlockEntity)) {
                LOGGER.trace("[cheststack] Block below {} is not a chest. Done.", cur);
                break;
            }

            List<BlockPos> blocksForChest = getChestBlocks(below, world);
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                for (BlockPos chestPos : blocksForChest) {
                    BlockPos signPos = chestPos.relative(dir);
                    BlockState signState = world.getBlockState(signPos);

                    LOGGER.trace("[cheststack] Checking chestPos {} and direction {} - pos {} for a sign.", chestPos, dir, signPos);

                    if (!(signState.getBlock() instanceof WallSignBlock)) continue;
                    if (!signPos.relative(signState.getValue(WallSignBlock.FACING).getOpposite()).equals(chestPos)) continue;

                    BlockEntity signBe = world.getBlockEntity(signPos);
                    if (!(signBe instanceof SignBlockEntity sign)) continue;
                    String line = findTextOnSign(sign, "\\[.+?]", true);
                    if (line == null) continue;

                    LOGGER.trace("[cheststack] Found category sign at {} - {}. Stopping stack here.", signPos, line);
                    Collections.reverse(result);
                    return result;
                }
            }

            cur = below;
        }

        Collections.reverse(result);
        return result;
    }

    private static List<ChestRef> findCategoryChests(SortContext context, ServerLevel world, String categoryName) {
        LOGGER.debug("[findCategoryChests] Searching for category '{}'", categoryName);

        SignBlockEntity targetSign = findClosestSignWithText(context, formatSignText(categoryName));

        if (targetSign == null) {
            LOGGER.debug("[findCategoryChests] No matching sign found for category '{}'", categoryName);
            return Collections.emptyList();
        }

        LOGGER.debug("[findCategoryChests] Nearest matching sign is at {}", targetSign.getBlockPos());

        BlockPos chestPos = getAttachedChestPos(targetSign.getBlockPos(), targetSign.getBlockState(), world);
        if (chestPos == null) {
            LOGGER.debug("[findCategoryChests] No chest attached to sign at {}", targetSign.getBlockPos());
            return Collections.emptyList();
        }

        LOGGER.debug("[findCategoryChests] Resolved chest position: {}", chestPos);

        List<ChestRef> stack = collectChestStack(world, chestPos);
        LOGGER.debug("[findCategoryChests] Found {} chest(s) in the stack.", stack.size());

        return stack;
    }

    private static class ChestRef {
        private final BlockPos pos;
        private final Container inventory;

        ChestRef(BlockPos pos, Container inv) {
            this.pos = pos;
            this.inventory = inv;
        }
    }

    /**
     * Per-command context that caches sign and container positions.
     * Built at command start and discarded after command completes.
     */
    private static class SortContext {
        // Maps sign text (e.g., "[food]") to the closest sign with that text
        private final Map<String, SignBlockEntity> signCache = new HashMap<>();
        // Maps block position to container for whereis command
        private final Map<BlockPos, Container> containerCache = new HashMap<>();
        private final ServerLevel world;
        private final BlockPos centerPos;
        private final int signRadius;
        private boolean signsScanned = false;

        SortContext(ServerLevel world, BlockPos centerPos, int signRadius) {
            this.world = world;
            this.centerPos = centerPos;
            this.signRadius = signRadius;
        }

        /**
         * Scans all signs in radius and caches their positions by text.
         * Only keeps the closest sign for each unique text.
         */
        private void buildSignCache() {
            if (signsScanned) return;
            signsScanned = true;

            BlockPos min = centerPos.offset(-signRadius, -signRadius, -signRadius);
            BlockPos max = centerPos.offset(signRadius, signRadius, signRadius);

            for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
                BlockEntity be = world.getBlockEntity(pos);
                BlockState state = world.getBlockState(pos);
                if (!(state.getBlock() instanceof WallSignBlock)) continue;
                if (!(be instanceof SignBlockEntity sign)) continue;

                // Get all text lines from the sign
                for (int i = 0; i < 4; i++) {
                    String frontLine = sign.getFrontText().getMessage(i, false).getString().trim();
                    String backLine = sign.getBackText().getMessage(i, false).getString().trim();

                    cacheSignText(frontLine, sign, pos);
                    cacheSignText(backLine, sign, pos);
                }
            }
            LOGGER.debug("[SortContext] Sign cache built with {} unique sign texts", signCache.size());
        }

        private void cacheSignText(String text, SignBlockEntity sign, BlockPos pos) {
            if (text.isEmpty()) return;
            String lowerText = text.toLowerCase();

            // Only keep the closest sign for each text
            SignBlockEntity existing = signCache.get(lowerText);
            if (existing == null) {
                signCache.put(lowerText, sign);
            } else {
                double existingDist = existing.getBlockPos().distSqr(centerPos);
                double newDist = pos.distSqr(centerPos);
                if (newDist < existingDist) {
                    signCache.put(lowerText, sign);
                }
            }
        }

        /**
         * Finds the closest sign with the given text, using the cache.
         */
        SignBlockEntity findSign(String text) {
            buildSignCache();
            return signCache.get(text.toLowerCase());
        }

        /**
         * Builds the container cache for whereis command.
         */
        void buildContainerCache() {
            BlockPos min = centerPos.offset(-signRadius, -signRadius, -signRadius);
            BlockPos max = centerPos.offset(signRadius, signRadius, signRadius);

            for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof Container inv) {
                    containerCache.put(pos.immutable(), inv);
                }
            }
            LOGGER.debug("[SortContext] Container cache built with {} containers", containerCache.size());
        }

        Map<BlockPos, Container> getContainerCache() {
            return containerCache;
        }
    }

    private static String summarize(Set<String> names, String message) {
        if (names == null || names.isEmpty()) {
            return "";
        }

        return (
                "\n"
                        + message + "\n"
                        + names.stream().sorted()
                        .map(name -> "- " + name)
                        .collect(Collectors.joining("\n"))
                        + "\n"
        );
    }

    private static class SortingResults {
        int sorted = 0;
        final Set<String> unknownItems = new HashSet<>();
        final Set<String> overflowCategories = new HashSet<>();
        final List<ItemStack> leftovers = new ArrayList<>();
        final Map<String, Integer> categoryCounts = new HashMap<>();
    }

    static class CategoryNode implements Comparable<CategoryNode> {
        String name;
        Set<String> includes = new HashSet<>();
        Set<ResourceLocation> itemIds = new HashSet<>();
        Set<ResourceLocation> flattenedItemIds = null;
        List<FilterRule> filters = new ArrayList<>();
        int priority = 10;

        static String categoriesToStr(Collection<CategoryNode> categories) {
            return categories.stream().map(CategoryNode::toString).collect(Collectors.joining(", "));
        }

        @Override
        public int compareTo(CategoryNode other) {
            return Integer.compare(priority, other.priority);
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CategoryNode)) return false;
            CategoryNode categoryNode = (CategoryNode) o;
            return name.equals(categoryNode.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }
}
