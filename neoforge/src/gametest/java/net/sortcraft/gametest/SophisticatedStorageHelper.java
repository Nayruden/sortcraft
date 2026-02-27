package net.sortcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.sortcraft.compat.IdentifierHelper;

import java.util.List;
import java.util.ArrayList;

final class SophisticatedStorageHelper {
    static final BlockPos INPUT_POS = new BlockPos(1, 1, 1);
    static final BlockPos FIRST_CATEGORY_POS = new BlockPos(3, 1, 1);
    static final BlockPos SECOND_CATEGORY_POS = new BlockPos(3, 1, 3);
    static final Direction DEFAULT_FACING = Direction.NORTH;

    static final String COBBLESTONE_CATEGORY = """
        cobblestone:
          items:
            - minecraft:cobblestone
        """;

    static final String SWORDS_CATEGORY = """
        swords:
          items:
            - minecraft:diamond_sword
            - minecraft:iron_sword
        """;

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

    static final String OAK_BARREL = "sophisticatedstorage:oak_barrel";
    static final String OAK_CHEST = "sophisticatedstorage:oak_chest";
    static final String IRON_BARREL = "sophisticatedstorage:iron_barrel";
    static final String IRON_CHEST = "sophisticatedstorage:iron_chest";

    private SophisticatedStorageHelper() {}

    static boolean isBlockAvailable(String blockId) {
        var id = IdentifierHelper.parse(blockId);
        if (!BuiltInRegistries.BLOCK.containsKey(id)) {
            return false;
        }
        return BuiltInRegistries.BLOCK.getValue(id) != Blocks.AIR;
    }

    /**
     * Pragmatic skip behavior for optional third-party mod tests.
     * Returns false after calling succeed() if the required block is unavailable.
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

    static void placeStorage(GameTestHelper helper, BlockPos pos, String blockId, Direction facing) {
        Block block = BuiltInRegistries.BLOCK.getValue(IdentifierHelper.parse(blockId));
        if (block == Blocks.AIR) {
            throw new IllegalStateException("Sophisticated Storage block not available for test: " + blockId);
        }

        BlockState state = block.defaultBlockState();
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
        } else if (state.hasProperty(BlockStateProperties.FACING)) {
            state = state.setValue(BlockStateProperties.FACING, facing);
        }
        helper.setBlock(pos, state);
    }

    static List<BlockPos> placeStorageStack(GameTestHelper helper, BlockPos bottomPos,
                                            int height, String blockId, Direction facing) {
        List<BlockPos> positions = new ArrayList<>();
        for (int i = 0; i < height; i++) {
            BlockPos pos = bottomPos.above(i);
            placeStorage(helper, pos, blockId, facing);
            positions.add(pos);
        }
        return positions;
    }

    static SortingTestSetup chestInputSophisticatedBarrelCategory(GameTestHelper helper, String categoryName) {
        TestHelper.placeSingleChest(helper, INPUT_POS, DEFAULT_FACING);
        TestHelper.placeInputSign(helper, INPUT_POS, DEFAULT_FACING);

        placeStorage(helper, FIRST_CATEGORY_POS, OAK_BARREL, DEFAULT_FACING);
        TestHelper.placeCategorySign(helper, FIRST_CATEGORY_POS, DEFAULT_FACING, categoryName);

        return new SortingTestSetup(INPUT_POS, List.of(FIRST_CATEGORY_POS));
    }

    static SortingTestSetup chestInputSophisticatedChestCategory(GameTestHelper helper, String categoryName) {
        TestHelper.placeSingleChest(helper, INPUT_POS, DEFAULT_FACING);
        TestHelper.placeInputSign(helper, INPUT_POS, DEFAULT_FACING);

        placeStorage(helper, FIRST_CATEGORY_POS, OAK_CHEST, DEFAULT_FACING);
        TestHelper.placeCategorySign(helper, FIRST_CATEGORY_POS, DEFAULT_FACING, categoryName);

        return new SortingTestSetup(INPUT_POS, List.of(FIRST_CATEGORY_POS));
    }

    static SortingTestSetup sophisticatedBarrelInputChestCategory(GameTestHelper helper, String categoryName) {
        placeStorage(helper, INPUT_POS, OAK_BARREL, DEFAULT_FACING);
        TestHelper.placeInputSign(helper, INPUT_POS, DEFAULT_FACING);

        TestHelper.placeSingleChest(helper, FIRST_CATEGORY_POS, DEFAULT_FACING);
        TestHelper.placeCategorySign(helper, FIRST_CATEGORY_POS, DEFAULT_FACING, categoryName);

        return new SortingTestSetup(INPUT_POS, List.of(FIRST_CATEGORY_POS));
    }

    static SortingTestSetup chestInputSophisticatedBarrelStackCategory(GameTestHelper helper,
                                                                       String categoryName,
                                                                       int stackHeight) {
        TestHelper.placeSingleChest(helper, INPUT_POS, DEFAULT_FACING);
        TestHelper.placeInputSign(helper, INPUT_POS, DEFAULT_FACING);

        List<BlockPos> stack = placeStorageStack(helper, FIRST_CATEGORY_POS, stackHeight, OAK_BARREL, DEFAULT_FACING);
        BlockPos top = stack.get(stack.size() - 1);
        TestHelper.placeCategorySign(helper, top, DEFAULT_FACING, categoryName);

        return new SortingTestSetup(INPUT_POS, stack);
    }

    static SortingTestSetup chestInputSophisticatedAndChestCategories(GameTestHelper helper,
                                                                      String sophisticatedCategory,
                                                                      String chestCategory) {
        TestHelper.placeSingleChest(helper, INPUT_POS, DEFAULT_FACING);
        TestHelper.placeInputSign(helper, INPUT_POS, DEFAULT_FACING);

        placeStorage(helper, FIRST_CATEGORY_POS, OAK_BARREL, DEFAULT_FACING);
        TestHelper.placeCategorySign(helper, FIRST_CATEGORY_POS, DEFAULT_FACING, sophisticatedCategory);

        TestHelper.placeSingleChest(helper, SECOND_CATEGORY_POS, DEFAULT_FACING);
        TestHelper.placeCategorySign(helper, SECOND_CATEGORY_POS, DEFAULT_FACING, chestCategory);

        return new SortingTestSetup(INPUT_POS, List.of(FIRST_CATEGORY_POS, SECOND_CATEGORY_POS));
    }
}
