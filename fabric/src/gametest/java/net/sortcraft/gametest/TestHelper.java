package net.sortcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.sortcraft.audit.AuditConfig;
import net.sortcraft.audit.ItemMovementRecord;
import net.sortcraft.audit.SortAuditEntry;
import net.sortcraft.audit.SortAuditLog;
import net.sortcraft.category.CategoryLoader;
import net.sortcraft.container.SortContext;
import net.sortcraft.sorting.SortingEngine;
import net.sortcraft.sorting.SortingResults;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Utility class for SortCraft GameTests.
 * Provides helper methods for chest/sign placement, item manipulation, and assertions.
 */
public final class TestHelper {
    private TestHelper() {}

    // ========== Category Setup ==========

    /**
     * Clears existing categories and loads new ones from YAML configurations.
     * Automatically calls flattenCategories() after loading.
     *
     * @param yamlConfigs One or more YAML configuration strings
     */
    public static void setupCategories(String... yamlConfigs) {
        CategoryLoader.clear();
        for (String yaml : yamlConfigs) {
            CategoryLoader.loadCategoriesFromYaml(yaml);
        }
        CategoryLoader.flattenCategories();
    }

    /**
     * Executes the sorting operation from a given input chest position.
     * This properly removes items from the input chest as they are sorted.
     *
     * @param helper    The GameTestHelper
     * @param inputPos  The relative position of the input chest
     * @param radius    The search radius for category chests
     * @return The SortingResults from the operation
     */
    public static SortingResults executeSort(GameTestHelper helper, BlockPos inputPos, int radius) {
        ServerLevel level = helper.getLevel();
        SortContext context = new SortContext(level, helper.absolutePos(inputPos), radius);
        Container inputContainer = getChestContainer(helper, inputPos);
        if (inputContainer == null) {
            throw new IllegalStateException("No chest container at " + inputPos);
        }
        return SortingEngine.sortFromContainer(context, level, inputContainer, false);
    }

    /**
     * Executes the sorting operation with default radius of 5 blocks.
     */
    public static SortingResults executeSort(GameTestHelper helper, BlockPos inputPos) {
        return executeSort(helper, inputPos, 5);
    }

    /**
     * Executes the sorting operation in preview mode (no actual changes).
     * This calculates what would be sorted without modifying any containers.
     *
     * @param helper    The GameTestHelper
     * @param inputPos  The relative position of the input chest
     * @param radius    The search radius for category chests
     * @return The SortingResults from the preview operation
     */
    public static SortingResults executeSortPreview(GameTestHelper helper, BlockPos inputPos, int radius) {
        ServerLevel level = helper.getLevel();
        SortContext context = new SortContext(level, helper.absolutePos(inputPos), radius);
        Container inputContainer = getChestContainer(helper, inputPos);
        if (inputContainer == null) {
            throw new IllegalStateException("No chest container at " + inputPos);
        }
        return SortingEngine.sortFromContainer(context, level, inputContainer, true);
    }

    /**
     * Executes the sorting operation in preview mode with default radius of 5 blocks.
     */
    public static SortingResults executeSortPreview(GameTestHelper helper, BlockPos inputPos) {
        return executeSortPreview(helper, inputPos, 5);
    }

    // ========== Chest Placement ==========

    /**
     * Places a single chest facing the given direction.
     */
    public static void placeSingleChest(GameTestHelper helper, BlockPos pos, Direction facing) {
        BlockState state = Blocks.CHEST.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
        helper.setBlock(pos, state);
    }

    /**
     * Places a double chest with secondary on the right side (default).
     * @param primaryPos Position of the chest half that will have the sign
     * @param facing Direction the chest faces (front of chest)
     */
    public static void placeDoubleChest(GameTestHelper helper, BlockPos primaryPos, Direction facing) {
        placeDoubleChest(helper, primaryPos, facing, true);
    }

    /**
     * Places a double chest. The primary position gets the sign, secondary is the other half.
     * @param primaryPos Position of the chest half that will have the sign
     * @param facing Direction the chest faces (front of chest)
     * @param secondaryOnRight If true, secondary chest is to the right of primary (when facing the chest)
     */
    public static void placeDoubleChest(GameTestHelper helper, BlockPos primaryPos,
                                        Direction facing, boolean secondaryOnRight) {
        // Determine chest types based on which side the secondary is on
        ChestType primaryType = secondaryOnRight ? ChestType.LEFT : ChestType.RIGHT;
        ChestType secondaryType = secondaryOnRight ? ChestType.RIGHT : ChestType.LEFT;

        // Calculate secondary position
        Direction secondaryDir = secondaryOnRight ? facing.getClockWise() : facing.getCounterClockWise();
        BlockPos secondaryPos = primaryPos.relative(secondaryDir);

        BlockState primaryState = Blocks.CHEST.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                .setValue(BlockStateProperties.CHEST_TYPE, primaryType);
        BlockState secondaryState = Blocks.CHEST.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                .setValue(BlockStateProperties.CHEST_TYPE, secondaryType);

        helper.setBlock(primaryPos, primaryState);
        helper.setBlock(secondaryPos, secondaryState);
    }

    /**
     * Places a vertical stack of single chests.
     * @param bottomPos Position of the bottom chest
     * @param height Number of chests in the stack
     * @param facing Direction each chest faces
     * @return List of chest positions from bottom to top
     */
    public static List<BlockPos> placeChestStack(GameTestHelper helper, BlockPos bottomPos,
                                                  int height, Direction facing) {
        List<BlockPos> positions = new ArrayList<>();
        for (int i = 0; i < height; i++) {
            BlockPos pos = bottomPos.above(i);
            placeSingleChest(helper, pos, facing);
            positions.add(pos);
        }
        return positions;
    }

    // ========== Sign Placement ==========

    /**
     * Places a wall sign on a chest with the given text on the front.
     * @param chestPos Position of the chest
     * @param signFacing Direction the sign faces (away from chest)
     * @param lines Text lines for the sign (up to 4)
     * @return The placed SignBlockEntity
     */
    public static SignBlockEntity placeSignOnChest(GameTestHelper helper, BlockPos chestPos,
                                                    Direction signFacing, String... lines) {
        BlockPos signPos = chestPos.relative(signFacing);
        BlockState signState = Blocks.OAK_WALL_SIGN.defaultBlockState()
                .setValue(WallSignBlock.FACING, signFacing);
        helper.setBlock(signPos, signState);

        SignBlockEntity sign = helper.getBlockEntity(signPos, SignBlockEntity.class);
        if (sign != null && lines != null) {
            for (int i = 0; i < Math.min(lines.length, 4); i++) {
                sign.setText(sign.getFrontText().setMessage(i, Component.literal(lines[i])), true);
            }
        }
        return sign;
    }

    /**
     * Places a sign on a chest with explicit 4 lines.
     * Alias for placeSignOnChest with all 4 lines specified.
     */
    public static SignBlockEntity placeSignOnChestMultiLine(GameTestHelper helper, BlockPos chestPos,
                                                             Direction signFacing, String line1, String line2,
                                                             String line3, String line4) {
        return placeSignOnChest(helper, chestPos, signFacing, line1, line2, line3, line4);
    }

    /**
     * Places a category sign on a chest (e.g., "[swords]").
     */
    public static SignBlockEntity placeCategorySign(GameTestHelper helper, BlockPos chestPos,
                                                     Direction signFacing, String categoryName) {
        return placeSignOnChest(helper, chestPos, signFacing, "[" + categoryName + "]");
    }

    /**
     * Places an input sign on a chest.
     */
    public static SignBlockEntity placeInputSign(GameTestHelper helper, BlockPos chestPos,
                                                  Direction signFacing) {
        return placeSignOnChest(helper, chestPos, signFacing, "[input]");
    }

    /**
     * Sets up a complete category chest stack with sign on top chest.
     * @param bottomPos Position of the bottom chest
     * @param height Number of chests
     * @param facing Direction chests face
     * @param signFacing Direction the category sign faces
     * @param categoryName Category name (without brackets)
     * @return List of chest positions from bottom to top
     */
    public static List<BlockPos> setupCategoryStack(GameTestHelper helper, BlockPos bottomPos,
                                                     int height, Direction facing,
                                                     Direction signFacing, String categoryName) {
        List<BlockPos> positions = placeChestStack(helper, bottomPos, height, facing);
        BlockPos topChest = positions.get(positions.size() - 1);
        placeCategorySign(helper, topChest, signFacing, categoryName);
        return positions;
    }

    // ========== Item Manipulation ==========

    /**
     * Inserts items into a chest at the given position.
     * @return true if all items were inserted successfully
     */
    public static boolean insertItems(GameTestHelper helper, BlockPos chestPos, ItemStack... items) {
        ServerLevel level = helper.getLevel();
        BlockPos absPos = helper.absolutePos(chestPos);
        BlockState state = level.getBlockState(absPos);

        if (!(state.getBlock() instanceof ChestBlock chestBlock)) {
            return false;
        }

        Container container = ChestBlock.getContainer(chestBlock, state, level, absPos, true);
        if (container == null) return false;

        int slot = 0;
        for (ItemStack item : items) {
            if (slot >= container.getContainerSize()) return false;
            container.setItem(slot++, item.copy());
        }
        return true;
    }

    /**
     * Inserts items into specific slots of a chest.
     */
    public static boolean insertItemAt(GameTestHelper helper, BlockPos chestPos, int slot, ItemStack item) {
        ServerLevel level = helper.getLevel();
        BlockPos absPos = helper.absolutePos(chestPos);
        BlockState state = level.getBlockState(absPos);

        if (!(state.getBlock() instanceof ChestBlock chestBlock)) {
            return false;
        }

        Container container = ChestBlock.getContainer(chestBlock, state, level, absPos, true);
        if (container == null || slot >= container.getContainerSize()) return false;

        container.setItem(slot, item.copy());
        return true;
    }

    /**
     * Fills a chest with the specified quantity of items.
     * Uses ItemQuantity to specify full stacks and partial stack amounts.
     *
     * @param helper   The GameTestHelper
     * @param chestPos Position of the chest to fill
     * @param quantity The ItemQuantity specifying what and how much to fill
     */
    public static void fillChest(GameTestHelper helper, BlockPos chestPos, ItemQuantity quantity) {
        int maxStack = quantity.maxStackSize();

        // Fill full stacks
        for (int i = 0; i < quantity.fullStacks(); i++) {
            insertItemAt(helper, chestPos, i, new ItemStack(quantity.item(), maxStack));
        }

        // Fill partial stack if any
        if (quantity.partialStackSize() > 0) {
            insertItemAt(helper, chestPos, quantity.fullStacks(),
                new ItemStack(quantity.item(), quantity.partialStackSize()));
        }
    }

    /**
     * Gets all non-empty items from a chest.
     */
    public static List<ItemStack> getChestContents(GameTestHelper helper, BlockPos chestPos) {
        ServerLevel level = helper.getLevel();
        BlockPos absPos = helper.absolutePos(chestPos);
        BlockState state = level.getBlockState(absPos);

        List<ItemStack> contents = new ArrayList<>();
        if (!(state.getBlock() instanceof ChestBlock chestBlock)) {
            return contents;
        }

        Container container = ChestBlock.getContainer(chestBlock, state, level, absPos, true);
        if (container == null) return contents;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                contents.add(stack.copy());
            }
        }
        return contents;
    }

    /**
     * Gets the item in a specific slot of a chest.
     */
    public static ItemStack getItemAt(GameTestHelper helper, BlockPos chestPos, int slot) {
        ServerLevel level = helper.getLevel();
        BlockPos absPos = helper.absolutePos(chestPos);
        BlockState state = level.getBlockState(absPos);

        if (!(state.getBlock() instanceof ChestBlock chestBlock)) {
            return ItemStack.EMPTY;
        }

        Container container = ChestBlock.getContainer(chestBlock, state, level, absPos, true);
        if (container == null || slot >= container.getContainerSize()) {
            return ItemStack.EMPTY;
        }

        return container.getItem(slot).copy();
    }

    /**
     * Counts total items of a specific type in a chest.
     */
    public static int countItemsInChest(GameTestHelper helper, BlockPos chestPos,
                                        net.minecraft.world.item.Item itemType) {
        List<ItemStack> contents = getChestContents(helper, chestPos);
        return contents.stream()
                .filter(stack -> stack.is(itemType))
                .mapToInt(ItemStack::getCount)
                .sum();
    }

    /**
     * Checks if a chest is completely empty.
     */
    public static boolean isChestEmpty(GameTestHelper helper, BlockPos chestPos) {
        return getChestContents(helper, chestPos).isEmpty();
    }

    /**
     * Gets the container for a chest (handles double chests).
     */
    public static Container getChestContainer(GameTestHelper helper, BlockPos chestPos) {
        ServerLevel level = helper.getLevel();
        BlockPos absPos = helper.absolutePos(chestPos);
        BlockState state = level.getBlockState(absPos);

        if (!(state.getBlock() instanceof ChestBlock chestBlock)) {
            return null;
        }

        return ChestBlock.getContainer(chestBlock, state, level, absPos, true);
    }

    // ========== Container Item Helpers (Bundle/Shulker) ==========

    /**
     * Creates a bundle containing the given items.
     */
    public static ItemStack createBundle(ItemStack... contents) {
        ItemStack bundle = new ItemStack(Items.BUNDLE);
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack item : contents) {
            if (!item.isEmpty()) {
                items.add(item.copy());
            }
        }
        bundle.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(items));
        return bundle;
    }

    /**
     * Creates a shulker box containing the given items.
     */
    public static ItemStack createShulkerBox(ItemStack... contents) {
        ItemStack shulker = new ItemStack(Items.SHULKER_BOX);
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack item : contents) {
            items.add(item.copy());
        }
        shulker.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
        return shulker;
    }

    /**
     * Gets the contents of a bundle.
     */
    public static List<ItemStack> getBundleContents(ItemStack bundle) {
        BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null) return new ArrayList<>();
        List<ItemStack> result = new ArrayList<>();
        contents.items().forEach(item -> result.add(item.copy()));
        return result;
    }

    /**
     * Gets the contents of a shulker box.
     */
    public static List<ItemStack> getShulkerContents(ItemStack shulker) {
        ItemContainerContents contents = shulker.get(DataComponents.CONTAINER);
        if (contents == null) return new ArrayList<>();
        List<ItemStack> result = new ArrayList<>();
        contents.stream().forEach(item -> result.add(item.copy()));
        return result;
    }

    // ========== Item Metadata Helpers ==========

    /**
     * Creates an ItemStack with a custom display name.
     */
    public static ItemStack namedStack(net.minecraft.world.item.Item item, String customName) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(customName));
        return stack;
    }

    /**
     * Creates an ItemStack with a custom name and count.
     */
    public static ItemStack namedStack(net.minecraft.world.item.Item item, int count, String customName) {
        ItemStack stack = new ItemStack(item, count);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(customName));
        return stack;
    }

    /**
     * Adds an enchantment to an ItemStack using the game's registry.
     * For game tests, use the helper.getLevel() to get registry access.
     *
     * @param helper The GameTestHelper for registry access
     * @param stack The ItemStack to enchant
     * @param enchantmentId The enchantment ID (e.g., "minecraft:sharpness")
     * @param level The enchantment level
     * @return The enchanted ItemStack (same instance, modified)
     */
    public static ItemStack enchant(GameTestHelper helper, ItemStack stack, String enchantmentId, int level) {
        ServerLevel serverLevel = helper.getLevel();
        net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.parse(enchantmentId);
        net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key =
                net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENCHANTMENT, id);

        var enchantmentRegistry = serverLevel.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        var holderOpt = enchantmentRegistry.get(key);

        if (holderOpt.isEmpty()) {
            throw new IllegalArgumentException("Unknown enchantment: " + enchantmentId);
        }

        net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> holder = holderOpt.get();

        // Get existing enchantments or create empty
        net.minecraft.world.item.enchantment.ItemEnchantments existing = stack.get(DataComponents.ENCHANTMENTS);
        if (existing == null) {
            existing = net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY;
        }

        // Create mutable, add enchantment, convert back to immutable
        net.minecraft.world.item.enchantment.ItemEnchantments.Mutable mutable =
                new net.minecraft.world.item.enchantment.ItemEnchantments.Mutable(existing);
        mutable.set(holder, level);
        stack.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());

        return stack;
    }

    /**
     * Creates a potion ItemStack with the specified potion type.
     *
     * @param helper The GameTestHelper for registry access
     * @param potionId The potion type ID (e.g., "minecraft:healing", "minecraft:strong_healing")
     * @return A potion ItemStack with the specified type
     */
    public static ItemStack createPotion(GameTestHelper helper, String potionId) {
        ServerLevel serverLevel = helper.getLevel();
        net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.parse(potionId);
        net.minecraft.resources.ResourceKey<net.minecraft.world.item.alchemy.Potion> key =
                net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.POTION, id);

        var potionRegistry = serverLevel.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.POTION);
        var holderOpt = potionRegistry.get(key);

        if (holderOpt.isEmpty()) {
            throw new IllegalArgumentException("Unknown potion: " + potionId);
        }

        ItemStack potion = new ItemStack(Items.POTION);
        potion.set(DataComponents.POTION_CONTENTS,
                new net.minecraft.world.item.alchemy.PotionContents(holderOpt.get()));
        return potion;
    }

    /**
     * Creates a splash potion ItemStack with the specified potion type.
     */
    public static ItemStack createSplashPotion(GameTestHelper helper, String potionId) {
        ServerLevel serverLevel = helper.getLevel();
        net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.parse(potionId);
        net.minecraft.resources.ResourceKey<net.minecraft.world.item.alchemy.Potion> key =
                net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.POTION, id);

        var potionRegistry = serverLevel.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.POTION);
        var holderOpt = potionRegistry.get(key);

        if (holderOpt.isEmpty()) {
            throw new IllegalArgumentException("Unknown potion: " + potionId);
        }

        ItemStack potion = new ItemStack(Items.SPLASH_POTION);
        potion.set(DataComponents.POTION_CONTENTS,
                new net.minecraft.world.item.alchemy.PotionContents(holderOpt.get()));
        return potion;
    }

    // ========== Assertions ==========

    /**
     * Asserts that a chest contains exactly the specified total count of an item type.
     */
    public static void assertChestContains(GameTestHelper helper, BlockPos chestPos,
                                           net.minecraft.world.item.Item itemType, int expectedCount) {
        int actual = countItemsInChest(helper, chestPos, itemType);
        if (actual != expectedCount) {
            helper.fail(Component.literal("Expected " + expectedCount + " of " + itemType +
                    " but found " + actual + " at " + chestPos));
        }
    }

    /**
     * Asserts that a chest is empty.
     */
    public static void assertChestEmpty(GameTestHelper helper, BlockPos chestPos) {
        if (!isChestEmpty(helper, chestPos)) {
            List<ItemStack> contents = getChestContents(helper, chestPos);
            helper.fail(Component.literal("Expected empty chest at " + chestPos +
                    " but found " + contents.size() + " item stacks"));
        }
    }

    /**
     * Asserts that a chest is NOT empty.
     */
    public static void assertChestNotEmpty(GameTestHelper helper, BlockPos chestPos) {
        if (isChestEmpty(helper, chestPos)) {
            helper.fail(Component.literal("Expected non-empty chest at " + chestPos));
        }
    }

    /**
     * Asserts the total number of non-empty stacks in a chest.
     */
    public static void assertChestStackCount(GameTestHelper helper, BlockPos chestPos, int expectedStacks) {
        int actual = getChestContents(helper, chestPos).size();
        if (actual != expectedStacks) {
            helper.fail(Component.literal("Expected " + expectedStacks + " stacks but found " +
                    actual + " at " + chestPos));
        }
    }

    /**
     * Asserts that an item in a chest slot matches expectations.
     */
    public static void assertSlotContains(GameTestHelper helper, BlockPos chestPos, int slot,
                                          net.minecraft.world.item.Item itemType, int expectedCount) {
        ItemStack actual = getItemAt(helper, chestPos, slot);
        if (actual.isEmpty() && expectedCount > 0) {
            helper.fail(Component.literal("Expected " + itemType + " x" + expectedCount +
                    " in slot " + slot + " but slot was empty"));
        } else if (!actual.is(itemType)) {
            helper.fail(Component.literal("Expected " + itemType + " in slot " + slot +
                    " but found " + actual.getItem()));
        } else if (actual.getCount() != expectedCount) {
            helper.fail(Component.literal("Expected " + expectedCount + " but found " +
                    actual.getCount() + " in slot " + slot));
        }
    }

    /**
     * Asserts that a slot is empty.
     */
    public static void assertSlotEmpty(GameTestHelper helper, BlockPos chestPos, int slot) {
        ItemStack actual = getItemAt(helper, chestPos, slot);
        if (!actual.isEmpty()) {
            helper.fail(Component.literal("Expected empty slot " + slot + " but found " +
                    actual.getItem() + " x" + actual.getCount()));
        }
    }

    /**
     * Asserts that a bundle contains a specific item.
     */
    public static void assertBundleContains(GameTestHelper helper, ItemStack bundle,
                                            net.minecraft.world.item.Item itemType, int expectedCount) {
        List<ItemStack> contents = getBundleContents(bundle);
        int total = contents.stream()
                .filter(s -> s.is(itemType))
                .mapToInt(ItemStack::getCount)
                .sum();
        if (total != expectedCount) {
            helper.fail(Component.literal("Expected bundle to contain " + expectedCount +
                    " of " + itemType + " but found " + total));
        }
    }

    /**
     * Asserts that a shulker contains a specific item.
     */
    public static void assertShulkerContains(GameTestHelper helper, ItemStack shulker,
                                             net.minecraft.world.item.Item itemType, int expectedCount) {
        List<ItemStack> contents = getShulkerContents(shulker);
        int total = contents.stream()
                .filter(s -> s.is(itemType))
                .mapToInt(ItemStack::getCount)
                .sum();
        if (total != expectedCount) {
            helper.fail(Component.literal("Expected shulker to contain " + expectedCount +
                    " of " + itemType + " but found " + total));
        }
    }

    // ========== Audit Testing Helpers ==========

    /**
     * Result holder for sort operations with audit data.
     */
    public record AuditedSortResult(SortingResults results, SortAuditEntry auditEntry) {}

    /**
     * Executes a sort operation with audit logging enabled.
     * Creates a mock player context for audit purposes.
     *
     * @param helper   The GameTestHelper
     * @param inputPos The relative position of the input chest
     * @param radius   The search radius for category chests
     * @return AuditedSortResult containing both sorting results and audit entry
     */
    public static AuditedSortResult executeSortWithAudit(GameTestHelper helper, BlockPos inputPos, int radius) {
        ServerLevel level = helper.getLevel();
        BlockPos absInputPos = helper.absolutePos(inputPos);
        SortContext context = new SortContext(level, absInputPos, radius);
        Container inputContainer = getChestContainer(helper, inputPos);
        if (inputContainer == null) {
            throw new IllegalStateException("No chest container at " + inputPos);
        }

        // Create audit log with test player info
        SortAuditLog audit = SortAuditLog.startForTest(
                "TestPlayer",
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                level.dimension().location().toString(),
                absInputPos,
                radius,
                false
        );

        SortingResults results = SortingEngine.sortFromContainer(context, level, inputContainer, false, audit);
        SortAuditEntry entry = audit.complete(results);

        return new AuditedSortResult(results, entry);
    }

    /**
     * Executes a sort operation with audit logging using default radius of 5.
     */
    public static AuditedSortResult executeSortWithAudit(GameTestHelper helper, BlockPos inputPos) {
        return executeSortWithAudit(helper, inputPos, 5);
    }

    /**
     * Asserts that a JSON string contains a specific key.
     */
    public static void assertJsonContains(GameTestHelper helper, String json, String key) {
        if (!json.contains("\"" + key + "\"")) {
            helper.fail(Component.literal("Expected JSON to contain key '" + key + "' but it didn't"));
        }
    }

    /**
     * Asserts that a JSON string does NOT contain a specific key.
     */
    public static void assertJsonNotContains(GameTestHelper helper, String json, String key) {
        if (json.contains("\"" + key + "\"")) {
            helper.fail(Component.literal("Expected JSON to NOT contain key '" + key + "' but it did"));
        }
    }

    /**
     * Validates audit entry at all three detail levels.
     * Returns the FULL json for additional assertions.
     */
    public static String validateAuditDetailLevels(GameTestHelper helper, SortAuditEntry entry,
                                                    boolean expectMovements, boolean expectCategorySummary) {
        String fullJson = entry.toJson(AuditConfig.DetailLevel.FULL);
        String summaryJson = entry.toJson(AuditConfig.DetailLevel.SUMMARY);
        String minimalJson = entry.toJson(AuditConfig.DetailLevel.MINIMAL);

        // Core fields should always be present
        for (String json : new String[]{fullJson, summaryJson, minimalJson}) {
            assertJsonContains(helper, json, "operationId");
            assertJsonContains(helper, json, "timestamp");
            assertJsonContains(helper, json, "playerName");
            assertJsonContains(helper, json, "status");
            assertJsonContains(helper, json, "totalItemsSorted");
        }

        // FULL should have movements if there are any
        if (expectMovements) {
            assertJsonContains(helper, fullJson, "movements");
        }
        // SUMMARY and MINIMAL should never have movements
        assertJsonNotContains(helper, summaryJson, "movements");
        assertJsonNotContains(helper, minimalJson, "movements");

        // FULL and SUMMARY should have categorySummary if there are any
        if (expectCategorySummary) {
            assertJsonContains(helper, fullJson, "categorySummary");
            assertJsonContains(helper, summaryJson, "categorySummary");
        }
        // MINIMAL should never have categorySummary
        assertJsonNotContains(helper, minimalJson, "categorySummary");

        return fullJson;
    }

    /**
     * Asserts that the audit entry's unknownItems contains all specified item IDs.
     * Throws AssertionError on failure to prevent helper.succeed() from being called.
     */
    public static void assertUnknownItemsContain(GameTestHelper helper, SortAuditEntry entry, String... expectedItems) {
        for (String item : expectedItems) {
            if (!entry.unknownItems().contains(item)) {
                throw new AssertionError("Expected unknown items to contain '" + item + "'");
            }
        }
    }

    /**
     * Asserts that the audit entry's overflowCategories contains all specified categories.
     * Throws AssertionError on failure to prevent helper.succeed() from being called.
     */
    public static void assertOverflowCategoriesContain(GameTestHelper helper, SortAuditEntry entry, String... expectedCategories) {
        for (String category : expectedCategories) {
            if (!entry.overflowCategories().contains(category)) {
                throw new AssertionError("Expected overflow categories to contain '" + category + "'");
            }
        }
    }

    /**
     * Asserts that the audit entry's movements contain an expected ItemMovementRecord.
     * Since the sorting engine records movements per-stack (not aggregated), this method
     * aggregates all movements with matching itemId, category, and destinationPos, then
     * compares the total quantity.
     * Throws AssertionError on failure to prevent helper.succeed() from being called.
     */
    public static void assertMovementExists(GameTestHelper helper, SortAuditEntry entry, ItemMovementRecord expected) {
        // Aggregate movements by itemId + category + destinationPos
        int totalQuantity = 0;
        boolean foundAny = false;
        for (var actual : entry.movements()) {
            if (actual.itemId().equals(expected.itemId()) &&
                actual.category().equals(expected.category()) &&
                actual.destinationPos().equals(expected.destinationPos())) {
                totalQuantity += actual.quantity();
                foundAny = true;
            }
        }

        if (!foundAny) {
            StringBuilder msg = new StringBuilder("No movements found for item: " + expected.itemId() +
                    " to category: " + expected.category() + " at position: " + expected.destinationPos());
            msg.append("\nActual movements:");
            for (var actual : entry.movements()) {
                msg.append("\n  - ").append(actual);
            }
            throw new AssertionError(msg.toString());
        }

        if (totalQuantity != expected.quantity()) {
            StringBuilder msg = new StringBuilder("Quantity mismatch for " + expected.itemId() +
                    " to " + expected.category() + " at " + expected.destinationPos() +
                    ": expected " + expected.quantity() + " but got " + totalQuantity);
            msg.append("\nActual movements:");
            for (var actual : entry.movements()) {
                msg.append("\n  - ").append(actual);
            }
            throw new AssertionError(msg.toString());
        }
    }

    /**
     * Asserts that the audit entry's movements contain all expected ItemMovementRecords.
     * Each expected record is compared by aggregating movements with matching itemId, category,
     * and destinationPos, then comparing the total quantity.
     * Throws AssertionError on failure to prevent helper.succeed() from being called.
     */
    public static void assertMovementsExist(GameTestHelper helper, SortAuditEntry entry, ItemMovementRecord... expectedMovements) {
        for (ItemMovementRecord expected : expectedMovements) {
            assertMovementExists(helper, entry, expected);
        }
    }

    /**
     * Creates an ItemMovementRecord for testing. Convenience method to construct records in tests.
     * The destinationPos should be an absolute position from helper.absolutePos().
     */
    public static ItemMovementRecord movement(String itemId, int quantity, String category, BlockPos destinationPos, boolean partial) {
        return new ItemMovementRecord(itemId, quantity, category, destinationPos, partial);
    }

    /**
     * Creates an ItemMovementRecord for testing with partial=false (the common case).
     */
    public static ItemMovementRecord movement(String itemId, int quantity, String category, BlockPos destinationPos) {
        return new ItemMovementRecord(itemId, quantity, category, destinationPos, false);
    }
}
