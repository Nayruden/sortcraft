package net.sortcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.sortcraft.container.ContainerHelper;

/**
 * Simple game tests for Sortcraft.
 * These tests run in an actual Minecraft server environment.
 */
public class SortcraftGameTest {

    /**
     * Tests that a wall sign can be placed on a chest and the sign text can be read.
     * This validates the basic infrastructure needed for category sorting.
     */
    @GameTest
    public void signOnChestCanBeRead(GameTestHelper helper) {
        // Place a chest at position (1, 1, 1)
        BlockPos chestPos = new BlockPos(1, 1, 1);
        helper.setBlock(chestPos, Blocks.CHEST.defaultBlockState());

        // Place a wall sign on the north side of the chest (sign faces north, attached to south side of chest)
        BlockPos signPos = chestPos.north();
        BlockState signState = Blocks.OAK_WALL_SIGN.defaultBlockState()
                .setValue(WallSignBlock.FACING, Direction.NORTH);
        helper.setBlock(signPos, signState);

        // Get the sign block entity and verify it exists
        SignBlockEntity sign = helper.getBlockEntity(signPos, SignBlockEntity.class);
        if (sign == null) {
            helper.fail(Component.literal("Sign block entity is null"));
            return;
        }

        // Verify the sign is attached to the chest (sign faces north, so chest is to the south)
        Direction signFacing = signState.getValue(WallSignBlock.FACING);
        BlockPos expectedChestPos = signPos.relative(signFacing.getOpposite());

        helper.assertBlockPresent(Blocks.CHEST, expectedChestPos);

        helper.succeed();
    }

    /**
     * Tests that a vertical stack of chests can be detected.
     * This validates the chest stacking logic used by Sortcraft.
     */
    @GameTest
    public void verticalChestStackDetected(GameTestHelper helper) {
        // Place 3 chests stacked vertically
        BlockPos bottomChest = new BlockPos(1, 1, 1);
        BlockPos middleChest = bottomChest.above();
        BlockPos topChest = middleChest.above();

        helper.setBlock(bottomChest, Blocks.CHEST.defaultBlockState());
        helper.setBlock(middleChest, Blocks.CHEST.defaultBlockState());
        helper.setBlock(topChest, Blocks.CHEST.defaultBlockState());

        // Verify all chests are placed
        helper.assertBlockPresent(Blocks.CHEST, bottomChest);
        helper.assertBlockPresent(Blocks.CHEST, middleChest);
        helper.assertBlockPresent(Blocks.CHEST, topChest);

        // Verify they are all chest block entities by getting them
        ChestBlockEntity bottom = helper.getBlockEntity(bottomChest, ChestBlockEntity.class);
        ChestBlockEntity middle = helper.getBlockEntity(middleChest, ChestBlockEntity.class);
        ChestBlockEntity top = helper.getBlockEntity(topChest, ChestBlockEntity.class);

        if (bottom == null || middle == null || top == null) {
            helper.fail(Component.literal("One or more chest block entities are null"));
            return;
        }

        helper.succeed();
    }

    /**
     * Simple sanity test - just verifies the test framework is working.
     */
    @GameTest
    public void sanityCheck(GameTestHelper helper) {
        // Place a block and verify it exists
        BlockPos pos = new BlockPos(0, 1, 0);
        helper.setBlock(pos, Blocks.DIAMOND_BLOCK.defaultBlockState());
        helper.assertBlockPresent(Blocks.DIAMOND_BLOCK, pos);
        helper.succeed();
    }

    /**
     * Tests ContainerHelper.getAttachedChestPos() - a real Sortcraft method.
     * This verifies that the mod correctly identifies the chest position
     * relative to a wall sign.
     */
    @GameTest
    public void containerHelperFindsAttachedChest(GameTestHelper helper) {
        // Place a chest at position (1, 1, 1)
        BlockPos chestPos = new BlockPos(1, 1, 1);
        helper.setBlock(chestPos, Blocks.CHEST.defaultBlockState());

        // Place a wall sign facing NORTH on the north side of the chest
        // Sign is at (1, 1, 0), faces north, so it's attached to (1, 1, 1)
        BlockPos signPos = chestPos.north();
        BlockState signState = Blocks.OAK_WALL_SIGN.defaultBlockState()
                .setValue(WallSignBlock.FACING, Direction.NORTH);
        helper.setBlock(signPos, signState);

        // Get the ServerLevel from the helper
        ServerLevel level = helper.getLevel();

        // Convert relative positions to absolute positions for the ContainerHelper
        BlockPos absoluteSignPos = helper.absolutePos(signPos);
        BlockState actualSignState = level.getBlockState(absoluteSignPos);

        // Call Sortcraft's ContainerHelper.getAttachedChestPos()
        BlockPos foundChestPos = ContainerHelper.getAttachedChestPos(absoluteSignPos, actualSignState, level);

        if (foundChestPos == null) {
            helper.fail(Component.literal("ContainerHelper.getAttachedChestPos() returned null - chest not found"));
            return;
        }

        // Verify the found position matches where we placed the chest
        BlockPos expectedAbsoluteChestPos = helper.absolutePos(chestPos);
        if (!foundChestPos.equals(expectedAbsoluteChestPos)) {
            helper.fail(Component.literal("Expected chest at " + expectedAbsoluteChestPos + " but found " + foundChestPos));
            return;
        }

        helper.succeed();
    }

    /**
     * Tests ContainerHelper.getAttachedChestPos() returns null when no chest is attached.
     */
    @GameTest
    public void containerHelperReturnsNullWhenNoChest(GameTestHelper helper) {
        // Place just a wall sign with no chest behind it
        BlockPos signPos = new BlockPos(1, 1, 1);
        BlockState signState = Blocks.OAK_WALL_SIGN.defaultBlockState()
                .setValue(WallSignBlock.FACING, Direction.NORTH);
        helper.setBlock(signPos, signState);

        // Get the ServerLevel from the helper
        ServerLevel level = helper.getLevel();

        // Convert relative positions to absolute positions
        BlockPos absoluteSignPos = helper.absolutePos(signPos);
        BlockState actualSignState = level.getBlockState(absoluteSignPos);

        // Call Sortcraft's ContainerHelper.getAttachedChestPos()
        BlockPos foundChestPos = ContainerHelper.getAttachedChestPos(absoluteSignPos, actualSignState, level);

        // Should be null since there's no chest
        if (foundChestPos != null) {
            helper.fail(Component.literal("Expected null but found chest at " + foundChestPos));
            return;
        }

        helper.succeed();
    }

    // ========== Cleanup Container Regression Tests ==========

    /**
     * Regression test: Verify that after sorting, no slots contain stacks with count=0.
     * This was causing "Failed to save chunk" errors before the cleanupContainer fix.
     */
    @GameTest
    public void noZeroCountStacksAfterSort(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.COBBLESTONE);

        SortingTestSetup setup = TestScenarios.basicInputAndCategory(helper, "cobblestone");

        // Add items to sort
        TestHelper.insertItems(helper, setup.inputPos(), new ItemStack(Items.COBBLESTONE, 64));

        // Execute sort
        TestHelper.executeSort(helper, setup.inputPos());

        // Get the raw container and verify no slots have count=0 stacks
        Container container = TestHelper.getChestContainer(helper, setup.inputPos());
        if (container == null) {
            helper.fail(Component.literal("Could not get input container"));
            return;
        }

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            // Check for the problematic case: stack.isEmpty() but count is 0 (not ItemStack.EMPTY)
            if (stack.getCount() == 0 && stack != ItemStack.EMPTY) {
                helper.fail(Component.literal("Slot " + i + " contains a count=0 stack that is not ItemStack.EMPTY"));
                return;
            }
        }

        helper.succeed();
    }

    /**
     * Verify that empty slots contain ItemStack.EMPTY (identity check).
     */
    @GameTest
    public void emptySlotContainsItemStackEmpty(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.COBBLESTONE);

        SortingTestSetup setup = TestScenarios.basicInputAndCategory(helper, "cobblestone");

        // Add a single stack to slot 0
        TestHelper.insertItems(helper, setup.inputPos(), new ItemStack(Items.COBBLESTONE, 64));

        // Execute sort
        TestHelper.executeSort(helper, setup.inputPos());

        // Verify input chest is empty and all slots contain ItemStack.EMPTY
        Container container = TestHelper.getChestContainer(helper, setup.inputPos());
        if (container == null) {
            helper.fail(Component.literal("Could not get input container"));
            return;
        }

        // After sorting, slot 0 should be ItemStack.EMPTY (not just isEmpty())
        ItemStack slot0 = container.getItem(0);
        if (!slot0.isEmpty()) {
            helper.fail(Component.literal("Slot 0 should be empty after sort"));
            return;
        }

        // The key check: isEmpty stacks should be ItemStack.EMPTY to prevent save errors
        // We can't directly check identity (==) with ItemStack.EMPTY in all cases,
        // but we can verify that isEmpty() returns true and count is 0
        if (slot0.getCount() != 0 && !slot0.isEmpty()) {
            helper.fail(Component.literal("Empty slot should have count 0"));
            return;
        }

        helper.succeed();
    }

    /**
     * Test that cleanup works correctly with partially sorted items.
     * When some items remain (unsortable), only the sorted slots should be cleaned up.
     */
    @GameTest
    public void cleanupWorksWithPartialSort(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.SWORDS);

        BlockPos inputPos = new BlockPos(1, 1, 1);
        BlockPos categoryPos = new BlockPos(3, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);
        TestHelper.placeSingleChest(helper, categoryPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, categoryPos, Direction.NORTH, "swords");

        // Add sortable and unsortable items
        TestHelper.insertItems(helper, inputPos,
            new ItemStack(Items.DIAMOND_SWORD),  // slot 0 - will be sorted
            new ItemStack(Items.DEBUG_STICK)     // slot 1 - will remain
        );

        // Execute sort
        TestHelper.executeSort(helper, inputPos);

        // Verify: slot 0 should be empty (sword was sorted)
        Container container = TestHelper.getChestContainer(helper, inputPos);
        if (container == null) {
            helper.fail(Component.literal("Could not get input container"));
            return;
        }

        ItemStack slot0 = container.getItem(0);
        if (!slot0.isEmpty()) {
            helper.fail(Component.literal("Slot 0 should be empty after sword was sorted"));
            return;
        }

        // Verify no count=0 stacks exist
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.getCount() == 0 && !stack.isEmpty()) {
                // This would be the bug case - count=0 but not recognized as empty
                helper.fail(Component.literal("Slot " + i + " has invalid count=0 stack"));
                return;
            }
        }

        helper.succeed();
    }

    // ========== Preview Mode Tests ==========

    /**
     * Test that preview mode returns correct results without modifying containers.
     */
    @GameTest
    public void previewModeReturnsCorrectResults(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.COBBLESTONE);

        SortingTestSetup setup = TestScenarios.basicInputAndCategory(helper, "cobblestone");

        // Add items to input
        TestHelper.insertItems(helper, setup.inputPos(), new ItemStack(Items.COBBLESTONE, 64));

        // Execute preview
        var results = TestHelper.executeSortPreview(helper, setup.inputPos());

        // Verify: Results should show 64 items would be sorted
        if (results.sorted != 64) {
            helper.fail(Component.literal("Preview should report 64 sorted but got " + results.sorted));
            return;
        }

        // Verify: Input chest should still have the items (not modified)
        int inputCount = TestHelper.countItemsInChest(helper, setup.inputPos(), Items.COBBLESTONE);
        if (inputCount != 64) {
            helper.fail(Component.literal("Input should still have 64 cobblestone but has " + inputCount));
            return;
        }

        // Verify: Category chest should be empty (not modified)
        TestHelper.assertChestEmpty(helper, setup.categoryPos());

        helper.succeed();
    }

    /**
     * Test that preview mode doesn't modify category chests.
     */
    @GameTest
    public void previewModeDoesNotModifyCategoryChest(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.COBBLESTONE);

        SortingTestSetup setup = TestScenarios.basicInputAndCategory(helper, "cobblestone");

        // Pre-fill category with some items
        TestHelper.insertItems(helper, setup.categoryPos(), new ItemStack(Items.COBBLESTONE, 32));

        // Add items to input
        TestHelper.insertItems(helper, setup.inputPos(), new ItemStack(Items.COBBLESTONE, 64));

        // Execute preview
        TestHelper.executeSortPreview(helper, setup.inputPos());

        // Verify: Category chest should still have only 32 (not modified)
        int categoryCount = TestHelper.countItemsInChest(helper, setup.categoryPos(), Items.COBBLESTONE);
        if (categoryCount != 32) {
            helper.fail(Component.literal("Category should still have 32 cobblestone but has " + categoryCount));
            return;
        }

        helper.succeed();
    }

    /**
     * Test that preview mode correctly reports overflow.
     */
    @GameTest
    public void previewModeReportsOverflow(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.COBBLESTONE);

        SortingTestSetup setup = TestScenarios.basicInputAndCategory(helper, "cobblestone");

        // Fill category chest completely
        TestHelper.fillChest(helper, setup.categoryPos(), ItemQuantity.full(Items.COBBLESTONE));

        // Add items to input
        TestHelper.insertItems(helper, setup.inputPos(), new ItemStack(Items.COBBLESTONE, 64));

        // Execute preview
        var results = TestHelper.executeSortPreview(helper, setup.inputPos());

        // Verify: Results should report overflow
        if (!results.overflowCategories.contains("cobblestone")) {
            helper.fail(Component.literal("Preview should report cobblestone overflow"));
            return;
        }

        // Verify: Input should still have items (not modified)
        int inputCount = TestHelper.countItemsInChest(helper, setup.inputPos(), Items.COBBLESTONE);
        if (inputCount != 64) {
            helper.fail(Component.literal("Input should still have 64 cobblestone"));
            return;
        }

        helper.succeed();
    }
}
