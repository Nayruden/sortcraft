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
import net.sortcraft.category.CategoryLoader;
import net.sortcraft.container.SortContext;
import net.sortcraft.sorting.SortingEngine;
import net.sortcraft.sorting.SortingResults;

import java.util.ArrayList;
import java.util.List;

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
}
