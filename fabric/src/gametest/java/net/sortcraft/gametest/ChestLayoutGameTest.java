package net.sortcraft.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.sortcraft.container.ContainerHelper;

import java.util.List;

/**
 * GameTests for chest layout configurations.
 * Tests single chests, double chests, and vertical stacks with various orientations.
 */
public class ChestLayoutGameTest {

    // ========== Single Chest Tests ==========

    /**
     * Test single chest facing NORTH with sign attached.
     */
    @GameTest
    public void singleChestFacingNorth(GameTestHelper helper) {
        BlockPos chestPos = new BlockPos(1, 1, 1);
        TestHelper.placeSingleChest(helper, chestPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, chestPos, Direction.NORTH, "test");

        helper.assertBlockPresent(Blocks.CHEST, chestPos);

        // Verify sign is attached and detects chest
        ServerLevel level = helper.getLevel();
        BlockPos absChestPos = helper.absolutePos(chestPos);
        BlockPos absSignPos = helper.absolutePos(chestPos.north());
        BlockState signState = level.getBlockState(absSignPos);

        BlockPos foundChest = ContainerHelper.getAttachedChestPos(absSignPos, signState, level);
        if (!absChestPos.equals(foundChest)) {
            helper.fail(Component.literal("Sign did not find attached chest"));
        }
        helper.succeed();
    }

    /**
     * Test single chest facing SOUTH with sign attached.
     */
    @GameTest
    public void singleChestFacingSouth(GameTestHelper helper) {
        BlockPos chestPos = new BlockPos(1, 1, 1);
        TestHelper.placeSingleChest(helper, chestPos, Direction.SOUTH);
        TestHelper.placeCategorySign(helper, chestPos, Direction.SOUTH, "test");

        ServerLevel level = helper.getLevel();
        BlockPos absChestPos = helper.absolutePos(chestPos);
        BlockPos absSignPos = helper.absolutePos(chestPos.south());
        BlockState signState = level.getBlockState(absSignPos);

        BlockPos foundChest = ContainerHelper.getAttachedChestPos(absSignPos, signState, level);
        if (!absChestPos.equals(foundChest)) {
            helper.fail(Component.literal("Sign did not find attached chest facing SOUTH"));
        }
        helper.succeed();
    }

    /**
     * Test single chest facing EAST with sign attached.
     */
    @GameTest
    public void singleChestFacingEast(GameTestHelper helper) {
        BlockPos chestPos = new BlockPos(1, 1, 1);
        TestHelper.placeSingleChest(helper, chestPos, Direction.EAST);
        TestHelper.placeCategorySign(helper, chestPos, Direction.EAST, "test");

        ServerLevel level = helper.getLevel();
        BlockPos absChestPos = helper.absolutePos(chestPos);
        BlockPos absSignPos = helper.absolutePos(chestPos.east());
        BlockState signState = level.getBlockState(absSignPos);

        BlockPos foundChest = ContainerHelper.getAttachedChestPos(absSignPos, signState, level);
        if (!absChestPos.equals(foundChest)) {
            helper.fail(Component.literal("Sign did not find attached chest facing EAST"));
        }
        helper.succeed();
    }

    /**
     * Test single chest facing WEST with sign attached.
     */
    @GameTest
    public void singleChestFacingWest(GameTestHelper helper) {
        BlockPos chestPos = new BlockPos(1, 1, 1);
        TestHelper.placeSingleChest(helper, chestPos, Direction.WEST);
        TestHelper.placeCategorySign(helper, chestPos, Direction.WEST, "test");

        ServerLevel level = helper.getLevel();
        BlockPos absChestPos = helper.absolutePos(chestPos);
        BlockPos absSignPos = helper.absolutePos(chestPos.west());
        BlockState signState = level.getBlockState(absSignPos);

        BlockPos foundChest = ContainerHelper.getAttachedChestPos(absSignPos, signState, level);
        if (!absChestPos.equals(foundChest)) {
            helper.fail(Component.literal("Sign did not find attached chest facing WEST"));
        }
        helper.succeed();
    }

    // ========== Double Chest Tests ==========

    /**
     * Test double chest facing NORTH, sign on primary (left) half.
     */
    @GameTest
    public void doubleChestNorthSignOnPrimary(GameTestHelper helper) {
        BlockPos primaryPos = new BlockPos(2, 1, 2);
        TestHelper.placeDoubleChest(helper, primaryPos, Direction.NORTH, true);
        TestHelper.placeCategorySign(helper, primaryPos, Direction.NORTH, "test");

        // Verify it's a double chest
        ServerLevel level = helper.getLevel();
        BlockPos absPos = helper.absolutePos(primaryPos);
        BlockState state = level.getBlockState(absPos);
        ChestType chestType = state.getValue(BlockStateProperties.CHEST_TYPE);

        if (chestType == ChestType.SINGLE) {
            helper.fail(Component.literal("Expected double chest but got single"));
            return;
        }

        // Verify the container has 54 slots (double chest)
        Container container = TestHelper.getChestContainer(helper, primaryPos);
        if (container == null || container.getContainerSize() != 54) {
            helper.fail(Component.literal("Expected 54 slots for double chest but got " +
                    (container == null ? "null" : container.getContainerSize())));
            return;
        }

        helper.succeed();
    }

    /**
     * Test double chest facing SOUTH, sign on secondary (right) half.
     */
    @GameTest
    public void doubleChestSouthSignOnSecondary(GameTestHelper helper) {
        BlockPos primaryPos = new BlockPos(2, 1, 2);
        TestHelper.placeDoubleChest(helper, primaryPos, Direction.SOUTH, true);

        // Put sign on the secondary half (to the right when facing south)
        Direction secondaryDir = Direction.SOUTH.getClockWise(); // WEST
        BlockPos secondaryPos = primaryPos.relative(secondaryDir);
        TestHelper.placeCategorySign(helper, secondaryPos, Direction.SOUTH, "test");

        // Container should still work from secondary position
        Container container = TestHelper.getChestContainer(helper, secondaryPos);
        if (container == null || container.getContainerSize() != 54) {
            helper.fail(Component.literal("Expected 54 slots from secondary position"));
            return;
        }

        helper.succeed();
    }

    /**
     * Test double chest facing EAST.
     */
    @GameTest
    public void doubleChestFacingEast(GameTestHelper helper) {
        BlockPos primaryPos = new BlockPos(2, 1, 2);
        TestHelper.placeDoubleChest(helper, primaryPos, Direction.EAST, false);
        TestHelper.placeCategorySign(helper, primaryPos, Direction.EAST, "test");

        Container container = TestHelper.getChestContainer(helper, primaryPos);
        if (container == null || container.getContainerSize() != 54) {
            helper.fail(Component.literal("Expected 54 slots for EAST-facing double chest"));
            return;
        }

        helper.succeed();
    }

    /**
     * Test double chest facing WEST.
     */
    @GameTest
    public void doubleChestFacingWest(GameTestHelper helper) {
        BlockPos primaryPos = new BlockPos(2, 1, 2);
        TestHelper.placeDoubleChest(helper, primaryPos, Direction.WEST, true);
        TestHelper.placeCategorySign(helper, primaryPos, Direction.WEST, "test");

        Container container = TestHelper.getChestContainer(helper, primaryPos);
        if (container == null || container.getContainerSize() != 54) {
            helper.fail(Component.literal("Expected 54 slots for WEST-facing double chest"));
            return;
        }

        helper.succeed();
    }

    /**
     * Test that getChestBlocks returns both positions for a double chest.
     */
    @GameTest
    public void doubleChestGetsBothBlocks(GameTestHelper helper) {
        BlockPos primaryPos = new BlockPos(2, 1, 2);
        TestHelper.placeDoubleChest(helper, primaryPos, Direction.NORTH, true);

        ServerLevel level = helper.getLevel();
        BlockPos absPos = helper.absolutePos(primaryPos);

        List<BlockPos> blocks = ContainerHelper.getChestBlocks(absPos, level);
        if (blocks.size() != 2) {
            helper.fail(Component.literal("Expected 2 blocks for double chest but got " + blocks.size()));
            return;
        }

        helper.succeed();
    }

    /**
     * Test that getChestBlocks returns 1 position for a single chest.
     */
    @GameTest
    public void singleChestGetsOneBlock(GameTestHelper helper) {
        BlockPos chestPos = new BlockPos(1, 1, 1);
        TestHelper.placeSingleChest(helper, chestPos, Direction.NORTH);

        ServerLevel level = helper.getLevel();
        BlockPos absPos = helper.absolutePos(chestPos);

        List<BlockPos> blocks = ContainerHelper.getChestBlocks(absPos, level);
        if (blocks.size() != 1) {
            helper.fail(Component.literal("Expected 1 block for single chest but got " + blocks.size()));
            return;
        }

        helper.succeed();
    }
}

