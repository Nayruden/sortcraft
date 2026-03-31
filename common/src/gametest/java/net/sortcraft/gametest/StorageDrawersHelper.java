package net.sortcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Self-contained helper for Storage Drawers GameTest integration.
 * Provides drawer placement, scenario setup, and category constants
 * without modifying the existing test infrastructure.
 */
public final class StorageDrawersHelper {
    private StorageDrawersHelper() {}

    // ========== Standard Test Positions ==========

    static final BlockPos INPUT_POS = new BlockPos(1, 1, 1);
    static final BlockPos FIRST_CATEGORY_POS = new BlockPos(3, 1, 1);
    static final BlockPos SECOND_CATEGORY_POS = new BlockPos(3, 1, 3);
    static final Direction DEFAULT_FACING = Direction.NORTH;

    // ========== Category YAML Constants ==========

    /** Single category: cobblestone */
    static final String COBBLESTONE_CATEGORY = """
        cobblestone:
          items:
            - minecraft:cobblestone
        """;

    /** Single category: building blocks (good for multi-slot drawer tests) */
    static final String BUILDING_CATEGORY = """
        building:
          items:
            - minecraft:cobblestone
            - minecraft:oak_planks
            - minecraft:stone_bricks
            - minecraft:glass
        """;

    /** Single category: ores/ingots */
    static final String ORES_CATEGORY = """
        ores:
          items:
            - minecraft:iron_ingot
            - minecraft:gold_ingot
            - minecraft:diamond
            - minecraft:emerald
        """;

    /** Single category: swords */
    static final String SWORDS_CATEGORY = """
        swords:
          items:
            - minecraft:diamond_sword
            - minecraft:iron_sword
        """;

    /** Two categories: building + ores */
    static final String BUILDING_AND_ORES = """
        building:
          items:
            - minecraft:cobblestone
            - minecraft:oak_planks
            - minecraft:stone_bricks
            - minecraft:glass
        ores:
          items:
            - minecraft:iron_ingot
            - minecraft:gold_ingot
            - minecraft:diamond
            - minecraft:emerald
        """;

    // ========== Drawer Block IDs ==========

    static final String OAK_FULL_DRAWERS_1 = "storagedrawers:oak_full_drawers_1";
    static final String OAK_FULL_DRAWERS_2 = "storagedrawers:oak_full_drawers_2";
    static final String OAK_FULL_DRAWERS_4 = "storagedrawers:oak_full_drawers_4";
    static final String OAK_HALF_DRAWERS_1 = "storagedrawers:oak_half_drawers_1";
    static final String OAK_HALF_DRAWERS_2 = "storagedrawers:oak_half_drawers_2";
    static final String OAK_HALF_DRAWERS_4 = "storagedrawers:oak_half_drawers_4";
    static final String OAK_COMP_DRAWERS_2 = "storagedrawers:compacting_drawers_2";
    static final String OAK_COMP_DRAWERS_3 = "storagedrawers:compacting_drawers_3";

    static boolean isBlockAvailable(String blockId) {
        var id = Identifier.parse(blockId);
        if (!BuiltInRegistries.BLOCK.containsKey(id)) {
            return false;
        }
        return BuiltInRegistries.BLOCK.getValue(id) != Blocks.AIR;
    }

    /**
     * Pragmatic skip behavior for optional third-party mod tests.
     * Returns false after calling succeed() if the required drawer block is unavailable.
     */
    static boolean requireAvailable(GameTestHelper helper, String... blockIds) {
        for (String blockId : blockIds) {
            if (!isBlockAvailable(blockId)) {
                helper.succeed();
                return false;
            }
        }
        return true;
    }

    static boolean requireAvailable(GameTestHelper helper) {
        return requireAvailable(helper, OAK_FULL_DRAWERS_1);
    }

    // ========== Drawer Placement ==========

    /**
     * Places a Storage Drawers block at the given position using registry lookup.
     * @param drawerId Full registry ID, e.g., "storagedrawers:oak_full_drawers_1"
     * @param facing Direction the drawer front faces
     */
    static void placeDrawer(GameTestHelper helper, BlockPos pos,
                            String drawerId, Direction facing) {
        var id = Identifier.parse(drawerId);
        Block block = BuiltInRegistries.BLOCK.getValue(id);
        if (block == Blocks.AIR) {
            throw new IllegalStateException("Storage Drawers block not available for test: " + drawerId);
        }
        BlockState state = block.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
        helper.setBlock(pos, state);
    }

    /** Places an oak full-depth 1-slot drawer. */
    static void placeDrawer1(GameTestHelper helper, BlockPos pos, Direction facing) {
        placeDrawer(helper, pos, OAK_FULL_DRAWERS_1, facing);
    }

    /** Places an oak full-depth 2-slot drawer. */
    static void placeDrawer2(GameTestHelper helper, BlockPos pos, Direction facing) {
        placeDrawer(helper, pos, OAK_FULL_DRAWERS_2, facing);
    }

    /** Places an oak full-depth 4-slot drawer. */
    static void placeDrawer4(GameTestHelper helper, BlockPos pos, Direction facing) {
        placeDrawer(helper, pos, OAK_FULL_DRAWERS_4, facing);
    }

    /** Places an oak half-depth drawer with the specified slot count. */
    static void placeHalfDrawer(GameTestHelper helper, BlockPos pos,
                                int slots, Direction facing) {
        String drawerId = "storagedrawers:oak_half_drawers_" + slots;
        placeDrawer(helper, pos, drawerId, facing);
    }

    /**
     * Places a vertical stack of drawers.
     * @return List of drawer positions from bottom to top
     */
    static List<BlockPos> placeDrawerStack(GameTestHelper helper, BlockPos bottomPos,
                                           int height, String drawerId, Direction facing) {
        List<BlockPos> positions = new ArrayList<>();
        for (int i = 0; i < height; i++) {
            BlockPos pos = bottomPos.above(i);
            placeDrawer(helper, pos, drawerId, facing);
            positions.add(pos);
        }
        return positions;
    }


    // ========== Scenario Setup ==========

    /**
     * Chest input → 1-slot drawer category.
     */
    static SortingTestSetup chestInputDrawer1Category(GameTestHelper helper, String categoryName) {
        TestHelper.placeSingleChest(helper, INPUT_POS, DEFAULT_FACING);
        TestHelper.placeInputSign(helper, INPUT_POS, DEFAULT_FACING);

        placeDrawer1(helper, FIRST_CATEGORY_POS, DEFAULT_FACING);
        TestHelper.placeCategorySign(helper, FIRST_CATEGORY_POS, DEFAULT_FACING, categoryName);

        return new SortingTestSetup(INPUT_POS, List.of(FIRST_CATEGORY_POS));
    }

    /**
     * Chest input → 2-slot drawer category.
     */
    static SortingTestSetup chestInputDrawer2Category(GameTestHelper helper, String categoryName) {
        TestHelper.placeSingleChest(helper, INPUT_POS, DEFAULT_FACING);
        TestHelper.placeInputSign(helper, INPUT_POS, DEFAULT_FACING);

        placeDrawer2(helper, FIRST_CATEGORY_POS, DEFAULT_FACING);
        TestHelper.placeCategorySign(helper, FIRST_CATEGORY_POS, DEFAULT_FACING, categoryName);

        return new SortingTestSetup(INPUT_POS, List.of(FIRST_CATEGORY_POS));
    }

    /**
     * Chest input → 4-slot drawer category.
     */
    static SortingTestSetup chestInputDrawer4Category(GameTestHelper helper, String categoryName) {
        TestHelper.placeSingleChest(helper, INPUT_POS, DEFAULT_FACING);
        TestHelper.placeInputSign(helper, INPUT_POS, DEFAULT_FACING);

        placeDrawer4(helper, FIRST_CATEGORY_POS, DEFAULT_FACING);
        TestHelper.placeCategorySign(helper, FIRST_CATEGORY_POS, DEFAULT_FACING, categoryName);

        return new SortingTestSetup(INPUT_POS, List.of(FIRST_CATEGORY_POS));
    }

    /**
     * 1-slot drawer input → chest category.
     */
    static SortingTestSetup drawer1InputChestCategory(GameTestHelper helper, String categoryName) {
        placeDrawer1(helper, INPUT_POS, DEFAULT_FACING);
        TestHelper.placeInputSign(helper, INPUT_POS, DEFAULT_FACING);

        TestHelper.placeSingleChest(helper, FIRST_CATEGORY_POS, DEFAULT_FACING);
        TestHelper.placeCategorySign(helper, FIRST_CATEGORY_POS, DEFAULT_FACING, categoryName);

        return new SortingTestSetup(INPUT_POS, List.of(FIRST_CATEGORY_POS));
    }

    /**
     * 1-slot drawer input → 1-slot drawer category.
     */
    static SortingTestSetup drawerInputDrawerCategory(GameTestHelper helper, String categoryName) {
        placeDrawer1(helper, INPUT_POS, DEFAULT_FACING);
        TestHelper.placeInputSign(helper, INPUT_POS, DEFAULT_FACING);

        placeDrawer1(helper, FIRST_CATEGORY_POS, DEFAULT_FACING);
        TestHelper.placeCategorySign(helper, FIRST_CATEGORY_POS, DEFAULT_FACING, categoryName);

        return new SortingTestSetup(INPUT_POS, List.of(FIRST_CATEGORY_POS));
    }

    /**
     * Chest input → half-depth drawer category.
     */
    static SortingTestSetup chestInputHalfDrawerCategory(GameTestHelper helper,
                                                          String categoryName, int slots) {
        TestHelper.placeSingleChest(helper, INPUT_POS, DEFAULT_FACING);
        TestHelper.placeInputSign(helper, INPUT_POS, DEFAULT_FACING);

        placeHalfDrawer(helper, FIRST_CATEGORY_POS, slots, DEFAULT_FACING);
        TestHelper.placeCategorySign(helper, FIRST_CATEGORY_POS, DEFAULT_FACING, categoryName);

        return new SortingTestSetup(INPUT_POS, List.of(FIRST_CATEGORY_POS));
    }

    /**
     * Chest input → vertical stack of drawers as category.
     * Sign is placed on the bottom drawer.
     */
    static SortingTestSetup chestInputDrawerStackCategory(GameTestHelper helper,
                                                           String categoryName,
                                                           int stackHeight, String drawerId) {
        TestHelper.placeSingleChest(helper, INPUT_POS, DEFAULT_FACING);
        TestHelper.placeInputSign(helper, INPUT_POS, DEFAULT_FACING);

        List<BlockPos> drawerPositions = placeDrawerStack(
                helper, FIRST_CATEGORY_POS, stackHeight, drawerId, DEFAULT_FACING);
        TestHelper.placeCategorySign(helper, FIRST_CATEGORY_POS, DEFAULT_FACING, categoryName);

        return new SortingTestSetup(INPUT_POS, drawerPositions);
    }

    /**
     * Chest input → two categories: one drawer and one chest.
     */
    static SortingTestSetup chestInputDrawerAndChestCategories(GameTestHelper helper,
                                                                String drawerCategory,
                                                                String chestCategory) {
        TestHelper.placeSingleChest(helper, INPUT_POS, DEFAULT_FACING);
        TestHelper.placeInputSign(helper, INPUT_POS, DEFAULT_FACING);

        placeDrawer1(helper, FIRST_CATEGORY_POS, DEFAULT_FACING);
        TestHelper.placeCategorySign(helper, FIRST_CATEGORY_POS, DEFAULT_FACING, drawerCategory);

        TestHelper.placeSingleChest(helper, SECOND_CATEGORY_POS, DEFAULT_FACING);
        TestHelper.placeCategorySign(helper, SECOND_CATEGORY_POS, DEFAULT_FACING, chestCategory);

        return new SortingTestSetup(INPUT_POS, List.of(FIRST_CATEGORY_POS, SECOND_CATEGORY_POS));
    }

    /**
     * Chest input → compacting drawer category.
     */
    static SortingTestSetup chestInputCompactingDrawerCategory(GameTestHelper helper,
                                                                String categoryName,
                                                                int compactingSlots) {
        TestHelper.placeSingleChest(helper, INPUT_POS, DEFAULT_FACING);
        TestHelper.placeInputSign(helper, INPUT_POS, DEFAULT_FACING);

        String drawerId = compactingSlots == 2 ? OAK_COMP_DRAWERS_2 : OAK_COMP_DRAWERS_3;
        placeDrawer(helper, FIRST_CATEGORY_POS, drawerId, DEFAULT_FACING);
        TestHelper.placeCategorySign(helper, FIRST_CATEGORY_POS, DEFAULT_FACING, categoryName);

        return new SortingTestSetup(INPUT_POS, List.of(FIRST_CATEGORY_POS));
    }
}
