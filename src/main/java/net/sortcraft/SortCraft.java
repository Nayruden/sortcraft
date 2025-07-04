package net.sortcraft;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.WallSignBlock;
import net.minecraft.block.entity.*;
import net.minecraft.block.enums.ChestType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SortCraft implements ModInitializer {
  public static final String MODID = "sortcraft";
  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger(MODID);
  private static final Map<String, CategoryNode> categories = new HashMap<>();
  private static final Map<Identifier, Set<CategoryNode>> itemCategoryMap = new HashMap<>();

  private static final String signPrefix = "[";
  private static final String signSuffix = "]";
  private static final String inputSignText = signPrefix + "input" + signSuffix;
  protected MinecraftServer server;

  @Override
  public void onInitialize() {
    ServerLifecycleEvents.SERVER_STARTING.register(server -> {
      loadCategoriesConfig(server);
      flattenCategories();
    });

    CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
      registerSortCommand(dispatcher);
    });

    UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
      if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;

      BlockPos pos = hitResult.getBlockPos();
      BlockState state = world.getBlockState(pos);
      if (!(state.getBlock() instanceof WallSignBlock)) return ActionResult.PASS;

      BlockEntity be = world.getBlockEntity(pos);
      if (!(be instanceof SignBlockEntity signBe)) return ActionResult.PASS;

      if (findTextOnSign(signBe, inputSignText) != null) {
        ServerCommandSource source = serverPlayer.getCommandSource();
        try {
          this.executeSortInput(source, false);
        } catch (Exception e) {
          e.printStackTrace();
        }

        return ActionResult.SUCCESS;
      }

      return ActionResult.PASS;
    });
  }

  @SuppressWarnings("unchecked")
  private void loadCategoriesConfig(MinecraftServer server) {
    this.server = server;
    Path configPath = FabricLoader.getInstance().getConfigDir().resolve("sortcraft").resolve("categories.yaml");
    try {
      if (!Files.exists(configPath)) {
        Files.createDirectories(configPath.getParent());
        String example = "# Example categories configuration\n"
                 + "example_category:\n"
                 + "  items:\n"
                 + "  - minecraft:iron_ingot\n"
                 + "  - minecraft:gold_ingot\n";
        Files.write(configPath, example.getBytes(StandardCharsets.UTF_8));
        LOGGER.warn("Category config not found, created example at {}", configPath);
      }
      Yaml yaml = new Yaml();
      try (InputStream in = Files.newInputStream(configPath)) {
        Object data = yaml.load(in);
        if (!(data instanceof Map)){
          LOGGER.warn("Categories config is not a map, no categories loaded");
          return;
        }
        Map<String, Object> map_root = (Map<String, Object>) data;
        for (Map.Entry<String, Object> entry : map_root.entrySet()) {
          String categoryName = entry.getKey();
          Object value_raw = entry.getValue();
          if (!(value_raw instanceof Map)) {
            LOGGER.warn("Category '{}' has a non-map value in YAML, skipping", categoryName);
            continue;
          }
          CategoryNode categoryNode = this._parseCategory(categoryName, value_raw);
          if ( categoryNode != null ) {
            categories.put(categoryName, categoryNode);
          }
        }
      }
      LOGGER.info("Loaded {} categories from config", categories.size());
    }
    catch ( IOException err ) {
      LOGGER.error("IO error while loading categories.yaml", err);
    }
    catch ( Exception err ) {
      LOGGER.error("Fatal error while loading categories.yaml", err);
    }
  }

  @SuppressWarnings("unchecked")
  private CategoryNode _parseCategory(String categoryName, Object value_raw) {
    try {
      CategoryNode categoryNode = new CategoryNode();
      categoryNode.name = categoryName;

      // category must be dictionary
      if ( !(value_raw instanceof Map<?,?>) ) {
        LOGGER.warn("Cannot parse category '{}', not a dictionary type", value_raw);
        return null;
      }
      Map<String, Object> category_conf = (Map<String, Object>) value_raw;


      // parse item patterns.
      // patterns naming a filter will be intersected (AND-ed) with other filters
      // other pattens will be unioned (OR'd) together.
      boolean anyFilter = false;
      var filteredItems = new HashSet<>(Registries.ITEM.getIds());
      Object itempatterns_value = category_conf.get("items");
      if ( itempatterns_value instanceof List<?> itempatterns ) {
        for ( var pattern_raw : itempatterns ) {
          switch ( pattern_raw ) {
            //
            // regex pattern as string
            case String pattern_str
              when (
              pattern_str.charAt(0) == '/' && pattern_str.charAt(pattern_str.length()-1) == '/'
            ) -> {
              var pattern = Pattern.compile(pattern_str.substring(1,pattern_str.length()-1));

              // clunky: constantly converting identifier to string before matching regexp
              Registries.ITEM.getIds().stream()
                .filter( item_id -> pattern.matcher(item_id.toString()).find() )
                .forEach(categoryNode.itemIds::add);
            }

            //
            // identifier as string
            case String item_name -> {
              Identifier id = Identifier.tryParse(item_name);
              if ( id != null ) {
                categoryNode.itemIds.add(id);
              }
              else {
                LOGGER.warn("Invalid item identifier '{}' in category '{}'", item_name, categoryName);
              }
            }

            //
            // complex filter as object
            case Map<?,?> filter_attrs -> {
              anyFilter = true;
              var filters = (
                filter_attrs.entrySet().stream()
                  .map((entry) -> FilterRuleFactory.fromYaml(this.server, (String)entry.getKey(), (String) entry.getValue()))
                  .toList()
              );

              // clunky: allocating an ItemStack for every entry
              filteredItems = (
                filteredItems.stream()
                  .filter(id ->
                    filters.stream()
                      .allMatch( f -> f.matches(new ItemStack(Registries.ITEM.getEntry(id).orElseThrow())))
                  )
                  .collect(Collectors.toCollection(HashSet::new))
              );
            }


            default -> LOGGER.warn("Unhandled item pattern type '{}' in category '{}'", pattern_raw, categoryName);

          }
        }
      }
      else if ( itempatterns_value != null ) {
        LOGGER.warn("Category '{}' has unrecognized 'items' type '{}'", categoryName, itempatterns_value.getClass().getName());
      }

      // post item-patterns:
      if ( anyFilter && !filteredItems.isEmpty() ) {
        categoryNode.itemIds.addAll(filteredItems);
      }
      else if ( anyFilter ) {
        LOGGER.warn("Item patterns in category '{}' filtered out all items!", categoryName);
      }

      //
      // category includes
      Object include_raw = category_conf.get("includes");
      if ( include_raw instanceof List<?> include_categories ) {
        categoryNode.includes = new HashSet<>((List<String>) include_categories);
      }
      else if ( include_raw != null ) {
        LOGGER.warn("Category '{}' has unrecognized includes type {}", categoryName, include_raw.getClass().getName());
      }


      //
      // dynamic filters
      Object filters_raw = category_conf.get("filters");
      if (filters_raw instanceof List<?> filters) {
        for (Object filter_raw : filters) {
          if (filter_raw instanceof Map<?,?> filter_map) {
            var filter = (Map<String, String>) filter_map;
            for (Map.Entry<String, String> filter_entry : filter.entrySet())
              categoryNode.filters.add(FilterRuleFactory.fromYaml(this.server, filter_entry.getKey(), filter_entry.getValue()));
          }
        }
      }
      else if ( filters_raw != null ) {
        LOGGER.warn("Category '{}' has unrecognized filter type {}", categoryName, filters_raw.getClass().getName());
      }

      //
      // priority
      Object priority_raw = category_conf.get("priority");
      if ( priority_raw instanceof Integer priority ) {
        categoryNode.priority = (int) priority;
      }
      else if ( priority_raw != null ) {
        LOGGER.warn("Category '{}' has unrecognized priority type {}", categoryName, priority_raw.getClass().getName());
      }

      return categoryNode;
    }
    catch ( Exception err ) {
      LOGGER.warn(String.format("Failed to parse category '%s'", categoryName), err);
      return null;
    }
  }

  private Set<Identifier> flattenCategory(String categoryName) {
    return flattenCategory(categoryName, new HashSet<>());
  }

  private Set<Identifier> flattenCategory(String categoryName, Set<String> visitedCategories) {
    CategoryNode category = categories.get(categoryName);
    if (category == null) throw new IllegalArgumentException("Unknown category: " + categoryName);

    Set<Identifier> items = category.flattened_itemIds;
    if (items != null) return items; // Already flattened

    if (visitedCategories.contains(categoryName)) throw new IllegalStateException("Cycle detected: " + categoryName);
    visitedCategories.add(categoryName);

    items = new HashSet<>();
    category.flattened_itemIds = items;

    items.addAll(category.itemIds);
    for (String childName : category.includes) items.addAll(flattenCategory(childName, visitedCategories));

    return items;
  }

  private void flattenCategories() {
    // Calculate forward mappings (include recursive includes)
    for (String categoryName : categories.keySet()) flattenCategory(categoryName);

    // Calculate backward mappings
    for (CategoryNode category : categories.values()) {
      for (Identifier itemId : category.flattened_itemIds)
        itemCategoryMap.computeIfAbsent(itemId, k -> new HashSet<CategoryNode>()).add(category);
    }
  }

  private void registerSortCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
    dispatcher.register(
      CommandManager.literal("sort")
        .then(CommandManager.literal("input")
          .executes(ctx -> executeSortInput(ctx.getSource(), false)))
        .then(CommandManager.literal("preview")
          .executes(ctx -> executeSortInput(ctx.getSource(), true)))
        .then(CommandManager.literal("diagnostics")
          .executes(this::executeSortDiag))
        .then(CommandManager.literal("whereis")
          .then(CommandManager.argument("item", StringArgumentType.greedyString())
            .suggests((context, builder) -> {
              // Provide suggestions based on available item registry
              for (Identifier id : Registries.ITEM.getIds()) {
                builder.suggest(id.toString());
              }
              return builder.buildFuture();
            })
            .executes(ctx -> executeSortWhereIs(ctx))))
        .then(CommandManager.literal("category")
          .then(CommandManager.argument("item", StringArgumentType.greedyString())
            .suggests((context, builder) -> {
              for (Identifier id : Registries.ITEM.getIds()) {
                builder.suggest(id.toString());
              }
              return builder.buildFuture();
            })
            .executes(ctx -> executeSortCategory(ctx))))
        .then(CommandManager.literal("help")
          .executes(this::executeSortHelp))
        .then(CommandManager.literal("dump")
            .executes(this::dumpItemTags))
        .then(CommandManager.literal("reload")
          .executes(this::executeSortReload))
    );
  }


  private int dumpItemTags(CommandContext<ServerCommandSource> context) {
    try {
      Map<String, List<String>> itemsToTags = new HashMap<>();
      Map<String, List<String>> tagsToItems = new HashMap<>();

      for (Identifier id : Registries.ITEM.getIds()) {
        Item item = Registries.ITEM.get(id);
        RegistryEntry<Item> entry = Registries.ITEM.getEntry(item);

        Collection<TagKey<Item>> tags = entry.streamTags().toList();
        List<String> tagList = new ArrayList<>();

        for (TagKey<Item> tag : tags) {
          String tagStr = tag.id().toString();
          tagList.add(tagStr);

          // Build tagsToItems reverse map
          tagsToItems.computeIfAbsent(tagStr, k -> new ArrayList<>()).add(id.toString());
        }

        itemsToTags.put(id.toString(), tagList);
      }

      // Write items_to_tags.json
      Path itemsToTagsPath = FabricLoader.getInstance().getConfigDir().resolve("sortcraft").resolve("items_to_tags.json");
      try (FileWriter writer = new FileWriter(itemsToTagsPath.toFile())) {
        writer.write(new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(itemsToTags));
      }

      // Write tags_to_items.json
      Path tagsToItemsPath = FabricLoader.getInstance().getConfigDir().resolve("sortcraft").resolve("tags_to_items.json");
      try (FileWriter writer = new FileWriter(tagsToItemsPath.toFile())) {
        writer.write(new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(tagsToItems));
      }

      context.getSource().sendFeedback(() -> Text.literal("Dumped items_to_tags.json and tags_to_items.json"), false);
    } catch (Exception e) {
      e.printStackTrace();
      context.getSource().sendError(Text.literal("Error dumping item tags: " + e.getMessage()));
    }
    return 1;
  }

  private int executeSortHelp(CommandContext<ServerCommandSource> context) {
    String helpMessage = String.format("""
      Sort Command Help:
      /sort input           - Sorts items from the closest input chest (chest must have a sign with '%s')
      /sort preview         - Shows a preview of what will be sorted and where
      /sort diagnostics     - Generates a diagnostics report as YAML
      /sort whereis <item>  - Finds chests that contain the specified item
      /sort category <item> - Shows the sorting category for the specified item
      /sort help            - Shows this help message

      All commands support autocomplete. Use TAB for suggestions.
      """, inputSignText);
    context.getSource().sendFeedback(() -> Text.literal(helpMessage), false);
    return 1;
  }

  private int executeSortReload(CommandContext<ServerCommandSource> context) {
    ServerCommandSource source = context.getSource();
    MinecraftServer server = source.getServer();

    try {
      categories.clear();
      itemCategoryMap.clear();
      loadCategoriesConfig(server);
      flattenCategories();

      source.sendFeedback(() -> Text.literal("SortCraft categories reloaded successfully."), false);
      LOGGER.info("[sortreload] Categories reloaded successfully.");
      return 1;
    } catch (Exception e) {
      source.sendError(Text.literal("Error reloading categories: " + e.getMessage()));
      LOGGER.error("[sortreload] Failed to reload categories.", e);
      return 0;
    }
  }

  private String findTextOnSign(SignBlockEntity sign, String text) {
    return findTextOnSign(sign, text, false);
  }

  // Checks if sign has text and returns the line if so
  private String findTextOnSign(SignBlockEntity sign, String text, boolean isRegex) {
    Text[] lines = sign.getFrontText().getMessages(false);
    for (Text line : lines) {
      if (line == null) continue;

      String line_text = line.getString().trim().toLowerCase();
      if (!isRegex) {
        if (line_text.contains(text)) return line_text;
      } else {
        if (line_text.matches(text)) return line_text;
      }
    }

    return null;
  }

  private SignBlockEntity findClosestSignWithText(ServerWorld world, BlockPos startingPos, String text, int search_distance) {
    double closestDistSq = Double.MAX_VALUE;
    SignBlockEntity closestSign = null;
    int radius = 20;
    BlockPos min = startingPos.add(-radius, -radius, -radius);
    BlockPos max = startingPos.add(radius, radius, radius);

    for (BlockPos pos : BlockPos.iterate(min, max)) {
      BlockEntity be = world.getBlockEntity(pos);
      BlockState state = world.getBlockState(pos);
      if (!(state.getBlock() instanceof WallSignBlock)) continue;
      if (!(be instanceof SignBlockEntity sign)) continue;

      String line = findTextOnSign(sign, text);
      if (line == null) continue;

      LOGGER.info("[findClosestSignWithText] Found possible matching sign at {} with text '{}'", pos, line);
      double distSq = pos.getSquaredDistance(startingPos.toCenterPos());
      if (distSq < closestDistSq) {
        closestDistSq = distSq;
        closestSign = sign;
      }
    }

    return closestSign;
  }

  List<CategoryNode> getMatchingCategoriesNoFilter(Identifier itemId) {
    Set<CategoryNode> categoriesRaw = itemCategoryMap.get(itemId);
    if (categoriesRaw == null) return new ArrayList<>();
    List<CategoryNode> categories = new ArrayList<>(categoriesRaw);
    Collections.sort(categories);
    return categories;
  }

  List<CategoryNode> getMatchingCategories(ItemStack stack) {
    Identifier itemId = Registries.ITEM.getId(stack.getItem());
    List<CategoryNode> filteredCategories = new ArrayList<>();
    List<CategoryNode> categories = getMatchingCategoriesNoFilter(itemId);

    for (CategoryNode category : categories) {
      if (category.filters.stream().allMatch(f -> f.matches(stack))) filteredCategories.add(category);
    }

    Collections.sort(filteredCategories);
    return filteredCategories;
  }

  private Identifier getSingleItemIfUniformAndMeetsThreshold(Iterable<ItemStack> stacks, int threshold) {
    Identifier singleItem = null;
    int count = 0;

    for (ItemStack stack : stacks) {
      if (stack.isEmpty()) continue;
      Identifier itemId = Registries.ITEM.getId(stack.getItem());

      if (singleItem == null) {
        singleItem = itemId;
      } else if (!singleItem.equals(itemId)) {
        return null; // Different items detected
      }
      count++;
    }

    if (count >= threshold) {
      return singleItem;
    }
    return null;
  }

  private SortingResults sortStacks(ServerWorld world,
      BlockPos inputPos,
      Iterable<ItemStack> stacks,
      boolean preview) {

    SortingResults sortingResults = new SortingResults();
    for (ItemStack stack: stacks) {
      if (stack.isEmpty()) continue;

      LOGGER.info("[sortinput] Sorting {} of {}", stack.getCount(), stack.getItem().toString());

      Iterable<ItemStack> inner_stacks = getStacksIfContainer(stack);
      if (inner_stacks != null) {
        // Check for uniform stacks optimization
        Identifier uniformItem = getSingleItemIfUniformAndMeetsThreshold(inner_stacks, 10);
        if (uniformItem != null) {
          LOGGER.info("[sortinput] Container has >=10 stacks of same item '{}'. Sorting container itself into that category.", uniformItem);

          List<CategoryNode> categories = getMatchingCategoriesNoFilter(uniformItem);
          sortStack(world, inputPos, preview, stack, categories, uniformItem, sortingResults);
          continue; // Skip inner sorting
        }

        LOGGER.info("[sortinput] Item is a container. Sorting contents of container.");
        SortingResults innerSortingResults = sortStacks(world, inputPos, inner_stacks, preview);
        sortingResults.sorted += innerSortingResults.sorted;
        sortingResults.overflowCategories.addAll(innerSortingResults.overflowCategories);
        sortingResults.unknownItems.addAll(innerSortingResults.unknownItems);
        innerSortingResults.categoryCounts.forEach((category, count) ->
          sortingResults.categoryCounts.merge(category, count, Integer::sum)
        );

        // Restore leftovers to the container
        if (!preview) {
          if (isBundle(stack)) {
            BundleContentsComponent bundle_contents;
            if (innerSortingResults.leftovers.isEmpty()) bundle_contents = new BundleContentsComponent(List.of());
            else bundle_contents = new BundleContentsComponent(innerSortingResults.leftovers);
            stack.set(DataComponentTypes.BUNDLE_CONTENTS, bundle_contents);
          } else { // Shulker
            DefaultedList<ItemStack> restored = DefaultedList.ofSize(27, ItemStack.EMPTY);
            for (int i = 0; i < innerSortingResults.leftovers.size() && i < 27; i++) {
              restored.set(i, innerSortingResults.leftovers.get(i));
            }
            stack.set(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(restored));
          }
        }

        if (!(innerSortingResults.leftovers.isEmpty())) {
          LOGGER.info("[sortinput] Inner container not completely emptied, giving up further sorting.");
          sortingResults.leftovers.add(stack);
          continue;
        }
      }

      Identifier itemId = Registries.ITEM.getId(stack.getItem());
      List<CategoryNode> categories = getMatchingCategories(stack);
      sortStack(world, inputPos, preview, stack, categories, itemId, sortingResults);
    }
    return sortingResults;
  }

  private void sortStack(ServerWorld world, BlockPos inputPos, boolean preview, ItemStack stack, List<CategoryNode> categories, Identifier itemId, SortingResults sortingResults) {
    if (categories.isEmpty()) {
      LOGGER.info("[sortinput] No categories found for item: {}", itemId);
      sortingResults.unknownItems.add(itemId.toString());
      sortingResults.leftovers.add(stack);
      return;
    }

    int stackSize = stack.getCount();
    int totalMoved = 0;
    String categoriesStr = CategoryNode.categoriesToStr(categories);
    for (CategoryNode category : categories) {
      List<ChestRef> categoryChests = findCategoryChests(world, inputPos, category.name);
      if (categoryChests.isEmpty()) continue;

      int moved = distributeToChests(stack, categoryChests, preview);
      totalMoved += moved;
      if (moved > 0) {
        sortingResults.sorted += moved;
        Map<String, Integer> counts = sortingResults.categoryCounts;
        counts.put(category.name, counts.getOrDefault(category.name, 0) + moved);
        LOGGER.info("[sortinput] Moved {} of item {}", moved, itemId);

        if (preview && totalMoved >= stackSize) break;
      }
    }

    if (totalMoved < stackSize) {
      LOGGER.warn("[sortinput] Overflow: Could not store (all of) item '{}' -> categories '{}'", itemId, categoriesStr);
      sortingResults.overflowCategories.add(categories.getFirst().name);
      sortingResults.leftovers.add(stack);
    }
  }

  private int executeSortInput(ServerCommandSource source, boolean preview) throws CommandSyntaxException {
    ServerWorld world = source.getWorld();
    BlockPos playerPos = source.getPlayer().getBlockPos();
    LOGGER.info("[sortinput] Starting sort near {}", playerPos);

    SignBlockEntity inputSign = findClosestSignWithText(world, playerPos, inputSignText, 20);

    if (inputSign == null) {
      source.sendFeedback(() -> Text.literal("No input sign found nearby."), false);
      LOGGER.warn("[sortinput] No input sign found within search radius.");
      return 0;
    }

    BlockPos chestPos = getAttachedChestPos(inputSign.getPos(), inputSign.getCachedState(), world);
    LOGGER.info("[sortinput] Attached chest position resolved: {}", chestPos);

    if (chestPos == null) {
      source.sendFeedback(() -> Text.literal("Input sign isn't attached to a chest."), false);
      LOGGER.warn("[sortinput] Sign at {} is not attached to a chest.", inputSign.getPos());
      return 0;
    }

    BlockState state = world.getBlockState(chestPos);
    if (!(state.getBlock() instanceof ChestBlock chestBlock)) {
      source.sendFeedback(() -> Text.literal("Block attached to the input sign is not a chest."), false);
      LOGGER.warn("[sortinput] Block at {} is not a ChestBlock.", chestPos);
      return 0;
    }

    Inventory inputInv = ChestBlock.getInventory(chestBlock, state, world, chestPos, true);
    if (inputInv == null) {
      source.sendFeedback(() -> Text.literal("Could not access input chest inventory."), false);
      LOGGER.warn("[sortinput] Failed to access chest inventory at {}", chestPos);
      return 0;
    }

    LOGGER.info("[sortinput] Input chest inventory loaded. Beginning sort.");

    SortingResults results = sortStacks(world, chestPos, inputInv, preview);

    for (String cat : results.overflowCategories) {
      source.sendFeedback(() -> Text.literal("⚠ Storage overflow in category: " + cat), false);
    }

    for (String itemId : results.unknownItems) {
      source.sendFeedback(() -> Text.literal("⚠ No category found for item: " + itemId), false);
    }

    if (preview) {
      Map<String, Integer> counts = results.categoryCounts;
      if (counts.isEmpty()) {
        source.sendFeedback(() -> Text.literal("No items to sort."), false);
      } else {
        source.sendFeedback(() -> Text.literal("Sort Preview:"), false);
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
          String cat = entry.getKey();
          int count = entry.getValue();
          source.sendFeedback(() -> Text.literal("- " + cat + ": " + count + " item" + (count != 1 ? "s" : "")), false);
        }
      }
      return 1;
    }

    if (results.sorted > 0) {
      world.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
          chestPos.getX() + 0.5,
          chestPos.getY() + 1.0,
          chestPos.getZ() + 0.5,
          10, 0.4, 0.5, 0.4, 0.1);
      String totalSortedStr = Integer.toString(results.sorted);
      source.sendFeedback(() -> Text.literal(totalSortedStr + " items sorted successfully."), false);
      LOGGER.info("[sortinput] Sorting complete. {} total items sorted. Particles triggered.", totalSortedStr);
    } else {
      source.sendFeedback(() -> Text.literal("No items were sorted."), false);
      LOGGER.info("[sortinput] No items were sorted.");
    }

    return 1;
  }

  private int executeSortWhereIs(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerCommandSource source = context.getSource();
    ServerWorld world = source.getWorld();
    BlockPos playerPos = source.getPlayer().getBlockPos();
    String itemName = StringArgumentType.getString(context, "item");

    Identifier itemId = Identifier.tryParse(itemName);
    if (itemId == null || !Registries.ITEM.containsId(itemId)) {
      source.sendError(Text.literal("Unknown item: " + itemName));
      return 0;
    }

    Item item = Registries.ITEM.get(itemId);
    Map<BlockPos, Iterable<ItemStack>> foundStorage = new HashMap<>();

    int radius = 64;
    BlockPos min = playerPos.add(-radius, -radius, -radius);
    BlockPos max = playerPos.add(radius, radius, radius);

    for (BlockPos pos : BlockPos.iterate(min, max)) {
      BlockEntity be = world.getBlockEntity(pos);
      if (be == null) continue;

      boolean isContainer = Stream.of(ChestBlockEntity.class, ShulkerBoxBlockEntity.class, BarrelBlockEntity.class)
        .anyMatch(cls -> cls.isInstance(be));
      if (isContainer) {
        if (!(be instanceof Inventory inv)) continue; // Some double checking error conditions
        foundStorage.put(pos.toImmutable(), inv);
      }
    }

    if (foundStorage.isEmpty()) {
      source.sendFeedback(() -> Text.literal("No nearby containers found."), false);
      return 1;
    }

    List<BlockPos> validPositions = foundStorage.entrySet().stream()
      .filter(entry -> stacksContainsItem(entry.getValue(), item))
      .sorted(Comparator.comparingDouble(entry -> entry.getKey().getSquaredDistance(playerPos)))
      .limit(3)
      .map(Map.Entry::getKey)
      .collect(Collectors.toList());

    if (validPositions.isEmpty()) {
      source.sendFeedback(() -> Text.literal("No " + itemName + " found in nearby containers."), false);
      return 1;
    }

    source.sendFeedback(() -> Text.literal("Item '" + itemName + "' found in:"), false);
    for (BlockPos pos : validPositions) {
      source.sendFeedback(() -> Text.literal("- " + pos.toShortString()), false);

      double x = pos.getX() + 0.5;
      double y = pos.getY() + 1.0;
      double z = pos.getZ() + 0.5;
      ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

      for (int i = 0; i < 10 * 20; i += 2) {
        long delayMillis = i * 50L;
        scheduler.schedule(() -> {
          world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 10, 0.5, 0.5, 0.5, 0.05);
        }, delayMillis, TimeUnit.MILLISECONDS);
      }
    }

    return 1;
  }

  private boolean stacksContainsItem(Iterable<ItemStack> stacks, Item item) {
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

  private int executeSortCategory(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerCommandSource source = context.getSource();
    String itemName = StringArgumentType.getString(context, "item");

    Identifier id = Identifier.tryParse(itemName);
    if (id == null || !Registries.ITEM.containsId(id)) {
      source.sendError(Text.literal("Unknown item: " + itemName));
      return 0;
    }
    Set<CategoryNode> categories = itemCategoryMap.get(id);
    if (!(categories == null || categories.isEmpty())) {
      String categoriesStr = CategoryNode.categoriesToStr(categories);
      source.sendFeedback(() -> Text.literal("Item " + id + " belongs to categories '" + categoriesStr + "'."), false);
    } else {
      source.sendFeedback(() -> Text.literal("Item " + id + " is not assigned to any category."), false);
    }
    return 1;
  }

  private int executeSortDiag(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerCommandSource source = context.getSource();
    ServerWorld world = source.getWorld();
    BlockPos playerPos = source.getPlayer().getBlockPos();
    Map<String, Map<String, Object>> categoryData = new TreeMap<>();

    for (Map.Entry<String, CategoryNode> entry : categories.entrySet()) {
      String categoryName = entry.getKey();
      CategoryNode categoryNode = entry.getValue();
      Set<Identifier> items = categoryNode.flattened_itemIds;

      if (items == null || items.isEmpty()) continue;

      List<ChestRef> chests = findCategoryChests(world, playerPos, categoryName);
      if (chests.isEmpty()) continue;

      Map<String, Object> itemData = new TreeMap<>();
      int totalSlots = chests.size() * 27;
      int usedSlots = 0;

      for (Identifier id : items) {
        Item item = Registries.ITEM.get(id);
        if (item == null) continue;

        float total = 0;
        Map<String, Float> locationCounts = new LinkedHashMap<>();

        for (ChestRef ref : chests) {
          Inventory inv = ref.inventory;
          float count = 0;
          for (int slot = 0; slot < inv.size(); slot++) {
            ItemStack stack = inv.getStack(slot);
            if (stack.getItem() == item) {
              count += stack.getCount();
              if (!stack.isEmpty()) usedSlots++;
            }
          }
          if (count > 0) {
            String loc = ref.pos.getX() + " " + ref.pos.getY() + " " + ref.pos.getZ();
            locationCounts.merge(loc, count, Float::sum);
            total += count;
          }
        }

        if (total > 0) {
          Map<String, Object> itemEntry = new LinkedHashMap<>();
          itemEntry.put("total_quantity", total);
          if (!locationCounts.isEmpty()) {
            List<Map<String, Object>> chestLocations = new ArrayList<>();
            for (Map.Entry<String, Float> e : locationCounts.entrySet()) {
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
      File file = new File(FabricLoader.getInstance().getConfigDir().toFile(), "sortcraft/sortdiag.yaml");
      file.getParentFile().mkdirs();

      DumperOptions options = new DumperOptions();
      options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
      options.setPrettyFlow(true);

      Yaml yaml = new Yaml(options);
      try (FileWriter writer = new FileWriter(file)) {
        yaml.dump(finalYaml, writer);
      }
    } catch (IOException e) {
      source.sendError(Text.literal("Failed to write sortcraft/sortdiag.yaml: " + e.getMessage()));
      return 0;
    }

    source.sendFeedback(() -> Text.literal("Sorter diagnostic written to sortcraft/sortdiag.yaml"), false);
    return 1;
  }

  private boolean isBundle(ItemStack stack) {
    return stack.getOrDefault(DataComponentTypes.BUNDLE_CONTENTS, null) != null;
  }

  private Iterable<ItemStack> getStacksIfContainer(ItemStack stack) {
    BundleContentsComponent bundle = stack.getOrDefault(DataComponentTypes.BUNDLE_CONTENTS, null);
    if (bundle != null) return bundle.iterate();

    ContainerComponent container = stack.getOrDefault(DataComponentTypes.CONTAINER, null);
    if (container != null) {
      DefaultedList<ItemStack> container_stacks = DefaultedList.ofSize(27, ItemStack.EMPTY);
      container.copyTo(container_stacks);
      return container_stacks;
    }

    return null;
  }

  private int distributeToChests(ItemStack stack, List<ChestRef> chests, boolean preview) {
    int originalCount = stack.getCount();
    int toSort = originalCount;
    int maxStackSize = Math.min(stack.getMaxCount(), 64);

    for (ChestRef ref : chests) {
      Inventory inv = ref.inventory;

      // Merge into existing stacks
      for (int slot = 0; slot < inv.size() && toSort > 0; slot++) {
        ItemStack target = inv.getStack(slot);
        if (!target.isEmpty() && ItemStack.areItemsAndComponentsEqual(stack, target) && target.getCount() < target.getMaxCount()) {
          int space = target.getMaxCount() - target.getCount();
          int move = Math.min(space, toSort);
          if (!preview) target.increment(move);
          toSort -= move;
        }
      }

      // Fill empty slots
      for (int slot = 0; slot < inv.size() && toSort > 0; slot++) {
        if (inv.getStack(slot).isEmpty()) {
          int move = Math.min(toSort, maxStackSize);
          if (!preview) {
            ItemStack toPut = stack.copy();
            toPut.setCount(move);
            inv.setStack(slot, toPut);
          }
          toSort -= move;
        }
      }

      if (toSort == 0) break;
    }

    int moved = originalCount - toSort;
    if (!preview) stack.decrement(moved);
    return moved;
  }

  private BlockPos getAttachedChestPos(BlockPos signPos, BlockState signState, ServerWorld world) {
    Direction attachedDirection = signState.get(Properties.HORIZONTAL_FACING, null);

    if (attachedDirection == null) return null;
    attachedDirection = attachedDirection.getOpposite();

    BlockPos chestPos = signPos.offset(attachedDirection);
    BlockEntity be = world.getBlockEntity(chestPos);
    if (be instanceof ChestBlockEntity) {
      return chestPos;
    }
    return null;
  }

  // Helper to return 1 or 2 block positions that make up a single/double chest
  private List<BlockPos> getChestBlocks(BlockPos pos, ServerWorld world) {
    BlockState state = world.getBlockState(pos);
    List<BlockPos> blocks = new ArrayList<>();
    blocks.add(pos);
    ChestType chestType = state.get(Properties.CHEST_TYPE, null);
    Direction facing = state.get(Properties.HORIZONTAL_FACING, null);

    if (chestType == null || facing == null || chestType == ChestType.SINGLE) return blocks;
    if (chestType == ChestType.RIGHT) {
      blocks.add(pos.offset(facing.rotateYCounterclockwise()));
    } else {
      blocks.add(pos.offset(facing.rotateYClockwise()));
    }
    return blocks;
  }

  private List<ChestRef> collectChestStack(ServerWorld world, BlockPos startPos) {
    List<ChestRef> result = new ArrayList<>();
    BlockPos cur = startPos;

    while (true) {
      BlockState state = world.getBlockState(cur);

      Block block = state.getBlock();
      if (block instanceof ChestBlock chestBlock) {
        Inventory inv = ChestBlock.getInventory(chestBlock, state, world, cur, true);
        if (inv != null) {
          result.add(new ChestRef(cur, inv));
          LOGGER.info("[cheststack] Added chest at {}", cur);
        }
      }
      //}

      // Check for category sign attached to the next chest *below*
      BlockPos below = cur.down();
      if (!(world.getBlockEntity(below) instanceof ChestBlockEntity)) {
        LOGGER.info("[cheststack] Block below {} is not a chest. Done.", cur);
        break;
      }

      List<BlockPos> blocksForChest = getChestBlocks(below, world);
      for (Direction dir : Direction.Type.HORIZONTAL) {
        for (BlockPos chestPos : blocksForChest) {
          BlockPos signPos = chestPos.offset(dir);
          BlockState signState = world.getBlockState(signPos);

          LOGGER.info("[cheststack] Checking chestPos {} and direction {} - pos {} for a sign.", chestPos, dir, signPos);

          if (!(signState.getBlock() instanceof WallSignBlock)) continue;
          // Only proceed if the sign is facing the chest
          if (!signPos.offset(signState.get(WallSignBlock.FACING).getOpposite()).equals(chestPos)) continue;

          BlockEntity signBe = world.getBlockEntity(signPos);
          if (!(signBe instanceof SignBlockEntity sign)) continue;
          String line = findTextOnSign(sign, "\\[.+?]", true);
          if (line == null) continue;

          LOGGER.info("[cheststack] Found category sign at {} - {}. Stopping stack here.", signPos, line);
          Collections.reverse(result);
          return result;
        }
      }

      cur = below;
    }

    Collections.reverse(result);
    return result;
  }

  private List<ChestRef> findCategoryChests(ServerWorld world, BlockPos nearPos, String categoryName) {
    LOGGER.info("[findCategoryChests] Searching signs near {} for category '{}'", nearPos, categoryName);

    SignBlockEntity targetSign = findClosestSignWithText(world, nearPos, signPrefix + categoryName + signSuffix, 64);

    if (targetSign == null) {
      LOGGER.info("[findCategoryChests] No matching sign found for category '{}'", categoryName);
      return Collections.emptyList();
    }

    LOGGER.info("[findCategoryChests] Nearest matching sign is at {}", targetSign.getPos());

    BlockPos chestPos = getAttachedChestPos(targetSign.getPos(), targetSign.getCachedState(), world);
    if (chestPos == null) {
      LOGGER.info("[findCategoryChests] No chest attached to sign at {}", targetSign.getPos());
      return Collections.emptyList();
    }

    LOGGER.info("[findCategoryChests] Resolved chest position: {}", chestPos);

    List<ChestRef> stack = collectChestStack(world, chestPos);
    LOGGER.info("[findCategoryChests] Found {} chest(s) in the stack.", stack.size());

    return stack;
  }

  private static class ChestRef {
    BlockPos pos;
    Inventory inventory;
    ChestRef(BlockPos pos, Inventory inv) {
      this.pos = pos;
      this.inventory = inv;
    }
  }

  private static class SortingResults {
    int sorted = 0;
    Set<String> unknownItems = new HashSet<String>();
    Set<String> overflowCategories = new HashSet<String>();
    List<ItemStack> leftovers = new ArrayList<ItemStack>();
    Map<String, Integer> categoryCounts  = new HashMap<String, Integer>();
  }

  class CategoryNode implements Comparable<CategoryNode> {
    String name;
    Set<String> includes = new HashSet<>();
    Set<Identifier> itemIds = new HashSet<>();
    Set<Identifier> flattened_itemIds = null;
    List<FilterRule> filters = new ArrayList<>();
    int priority = 10;

    static private String categoriesToStr(Collection<CategoryNode> categories) {
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
      return name == categoryNode.name;
    }

    @Override
    public int hashCode() {
      return Objects.hash(name);
    }
  }
}
