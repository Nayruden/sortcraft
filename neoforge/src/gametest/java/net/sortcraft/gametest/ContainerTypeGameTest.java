package net.sortcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.sortcraft.sorting.SortingResults;

import java.util.List;

/**
 * GameTests for generic container type support.
 * Verifies that sorting works with barrels, hoppers, dispensers, droppers,
 * and mixed container setups (not just chests).
 */
public class ContainerTypeGameTest {

    // ========== Barrel Tests ==========

    /**
     * Test sorting from a barrel input to a chest category.
     */
    public void barrelInputToChestCategory(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.SWORDS_SHORT);

        SortingTestSetup setup = TestScenarios.barrelInputChestCategory(helper, "swords");
        TestHelper.insertItems(helper, setup.inputPos(),
                new ItemStack(Items.DIAMOND_SWORD, 1),
                new ItemStack(Items.IRON_SWORD, 1));

        SortingResults results = TestHelper.executeSort(helper, setup.inputPos());
        if (results.sorted != 2) {
            helper.fail(Component.literal("Expected 2 items sorted from barrel but got " + results.sorted));
            return;
        }

        // Verify barrel is empty
        List<ItemStack> remaining = TestHelper.getChestContents(helper, setup.inputPos());
        if (!remaining.isEmpty()) {
            helper.fail(Component.literal("Barrel input should be empty but has " + remaining.size() + " items"));
            return;
        }

        helper.succeed();
    }

    /**
     * Test sorting from a chest input to a barrel category.
     */
    public void chestInputToBarrelCategory(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.SWORDS_SHORT);

        SortingTestSetup setup = TestScenarios.chestInputBarrelCategory(helper, "swords");
        TestHelper.insertItems(helper, setup.inputPos(),
                new ItemStack(Items.DIAMOND_SWORD, 1),
                new ItemStack(Items.IRON_SWORD, 1));

        SortingResults results = TestHelper.executeSort(helper, setup.inputPos());
        if (results.sorted != 2) {
            helper.fail(Component.literal("Expected 2 items sorted to barrel but got " + results.sorted));
            return;
        }

        // Verify items arrived in barrel category
        List<ItemStack> categoryContents = TestHelper.getChestContents(helper, setup.categoryPos());
        if (categoryContents.size() != 2) {
            helper.fail(Component.literal("Barrel category should have 2 items but has " + categoryContents.size()));
            return;
        }

        helper.succeed();
    }

    /**
     * Test sorting between barrels only (no chests at all).
     */
    public void barrelInputToBarrelCategory(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.COBBLESTONE);

        SortingTestSetup setup = TestScenarios.barrelInputBarrelCategory(helper, "cobblestone");
        TestHelper.insertItems(helper, setup.inputPos(),
                new ItemStack(Items.COBBLESTONE, 64));

        SortingResults results = TestHelper.executeSort(helper, setup.inputPos());
        if (results.sorted != 64) {
            helper.fail(Component.literal("Expected 64 items sorted barrel-to-barrel but got " + results.sorted));
            return;
        }

        helper.succeed();
    }

    // ========== Hopper Tests ==========

    /**
     * Test sorting from a hopper input to a chest category.
     * Hoppers have only 5 slots.
     */
    public void hopperInputToChestCategory(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.SWORDS_SHORT);

        SortingTestSetup setup = TestScenarios.hopperInputChestCategory(helper, "swords");
        TestHelper.insertItems(helper, setup.inputPos(),
                new ItemStack(Items.DIAMOND_SWORD, 1),
                new ItemStack(Items.IRON_SWORD, 1));

        SortingResults results = TestHelper.executeSort(helper, setup.inputPos());
        if (results.sorted != 2) {
            helper.fail(Component.literal("Expected 2 items sorted from hopper but got " + results.sorted));
            return;
        }

        // Verify hopper is empty
        List<ItemStack> remaining = TestHelper.getChestContents(helper, setup.inputPos());
        if (!remaining.isEmpty()) {
            helper.fail(Component.literal("Hopper input should be empty but has " + remaining.size() + " items"));
            return;
        }

        helper.succeed();
    }

    // ========== Dispenser Tests ==========

    /**
     * Test sorting from a chest input to a dispenser category.
     * Dispensers have 9 slots.
     */
    public void chestInputToDispenserCategory(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.SWORDS_SHORT);

        SortingTestSetup setup = TestScenarios.chestInputDispenserCategory(helper, "swords");
        TestHelper.insertItems(helper, setup.inputPos(),
                new ItemStack(Items.DIAMOND_SWORD, 1),
                new ItemStack(Items.IRON_SWORD, 1));

        SortingResults results = TestHelper.executeSort(helper, setup.inputPos());
        if (results.sorted != 2) {
            helper.fail(Component.literal("Expected 2 items sorted to dispenser but got " + results.sorted));
            return;
        }

        // Verify items arrived in dispenser category
        List<ItemStack> categoryContents = TestHelper.getChestContents(helper, setup.categoryPos());
        if (categoryContents.size() != 2) {
            helper.fail(Component.literal("Dispenser category should have 2 items but has " + categoryContents.size()));
            return;
        }

        helper.succeed();
    }

    // ========== Dropper Tests ==========

    /**
     * Test sorting from a chest input to a dropper category.
     * Droppers have 9 slots.
     */
    public void chestInputToDropperCategory(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.SWORDS_SHORT);

        SortingTestSetup setup = TestScenarios.chestInputDropperCategory(helper, "swords");
        TestHelper.insertItems(helper, setup.inputPos(),
                new ItemStack(Items.DIAMOND_SWORD, 1),
                new ItemStack(Items.IRON_SWORD, 1));

        SortingResults results = TestHelper.executeSort(helper, setup.inputPos());
        if (results.sorted != 2) {
            helper.fail(Component.literal("Expected 2 items sorted to dropper but got " + results.sorted));
            return;
        }

        // Verify items arrived in dropper category
        List<ItemStack> categoryContents = TestHelper.getChestContents(helper, setup.categoryPos());
        if (categoryContents.size() != 2) {
            helper.fail(Component.literal("Dropper category should have 2 items but has " + categoryContents.size()));
            return;
        }

        helper.succeed();
    }

    // ========== Mixed Container Tests ==========

    /**
     * Test sorting from a barrel input to multiple category types (chest, barrel, dispenser).
     */
    public void mixedContainerCategories(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.STORAGE);

        // Barrel as input
        BlockPos inputPos = new BlockPos(1, 1, 1);
        TestHelper.placeBarrel(helper, inputPos, Direction.UP);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);

        // Chest for swords
        BlockPos swordsPos = new BlockPos(3, 1, 1);
        TestHelper.placeSingleChest(helper, swordsPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, swordsPos, Direction.NORTH, "swords");

        // Barrel for pickaxes
        BlockPos pickaxesPos = new BlockPos(5, 1, 1);
        TestHelper.placeBarrel(helper, pickaxesPos, Direction.UP);
        TestHelper.placeCategorySign(helper, pickaxesPos, Direction.NORTH, "pickaxes");

        // Dropper for cobblestone
        BlockPos cobblestonePos = new BlockPos(7, 1, 1);
        TestHelper.placeDropper(helper, cobblestonePos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, cobblestonePos, Direction.NORTH, "cobblestone");

        // Insert mixed items into barrel input
        TestHelper.insertItems(helper, inputPos,
                new ItemStack(Items.DIAMOND_SWORD, 1),
                new ItemStack(Items.IRON_PICKAXE, 1),
                new ItemStack(Items.COBBLESTONE, 32));

        SortingResults results = TestHelper.executeSort(helper, inputPos, 10);
        if (results.sorted != 34) {
            helper.fail(Component.literal("Expected 34 items sorted across mixed containers but got " + results.sorted));
            return;
        }

        helper.succeed();
    }

    // ========== Vertical Stack Tests with Generic Containers ==========

    /**
     * Test vertical stacking of barrels as category containers.
     */
    public void barrelCategoryStack(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.COBBLESTONE);

        SortingTestSetup setup = TestScenarios.inputChestWithBarrelCategoryStack(helper, "cobblestone", 2);

        // Insert more cobblestone than one barrel can hold (27 slots * 64 = 1728 per barrel)
        // Insert 2 stacks across input
        TestHelper.insertItems(helper, setup.inputPos(),
                new ItemStack(Items.COBBLESTONE, 64),
                new ItemStack(Items.COBBLESTONE, 64));

        SortingResults results = TestHelper.executeSort(helper, setup.inputPos());
        if (results.sorted != 128) {
            helper.fail(Component.literal("Expected 128 items sorted to barrel stack but got " + results.sorted));
            return;
        }

        helper.succeed();
    }

    // ========== Trapped Chest Tests ==========

    /**
     * Test that trapped chests still work after generalization (regression test).
     */
    public void trappedChestStillWorks(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.SWORDS_SHORT);

        // Use trapped chest as input
        BlockPos inputPos = new BlockPos(1, 1, 1);
        helper.setBlock(inputPos, net.minecraft.world.level.block.Blocks.TRAPPED_CHEST.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);

        // Regular chest as category
        BlockPos categoryPos = new BlockPos(3, 1, 1);
        TestHelper.placeSingleChest(helper, categoryPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, categoryPos, Direction.NORTH, "swords");

        TestHelper.insertItems(helper, inputPos,
                new ItemStack(Items.DIAMOND_SWORD, 1),
                new ItemStack(Items.IRON_SWORD, 1));

        SortingResults results = TestHelper.executeSort(helper, inputPos);
        if (results.sorted != 2) {
            helper.fail(Component.literal("Expected 2 items sorted from trapped chest but got " + results.sorted));
            return;
        }

        helper.succeed();
    }
}

