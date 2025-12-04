package net.sortcraft.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * GameTests for complex storage room layouts.
 * Tests real-world configurations with multiple stacks and categories.
 */
public class StorageLayoutGameTest {

    /**
     * Test a typical storage tower: multiple category stacks side by side.
     */
    @GameTest
    public void storageTowerSideBySide(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.STORAGE);

        BlockPos inputPos = new BlockPos(1, 1, 1);

        // Create input chest
        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);

        // Create two category stacks side by side
        // Stack 1: swords (2 chests tall)
        BlockPos swordsBottom = new BlockPos(3, 1, 1);
        TestHelper.setupCategoryStack(helper, swordsBottom, 2, Direction.NORTH, Direction.NORTH, "swords");

        // Stack 2: pickaxes (2 chests tall) - adjacent to swords
        BlockPos pickaxesBottom = new BlockPos(5, 1, 1);
        TestHelper.setupCategoryStack(helper, pickaxesBottom, 2, Direction.NORTH, Direction.NORTH, "pickaxes");

        // Add items to input
        TestHelper.insertItems(helper, inputPos,
            new ItemStack(Items.DIAMOND_SWORD),
            new ItemStack(Items.IRON_SWORD),
            new ItemStack(Items.DIAMOND_PICKAXE),
            new ItemStack(Items.IRON_PICKAXE)
        );

        // Execute sort
        TestHelper.executeSort(helper, inputPos, 10);

        // Verify swords went to swords stack
        int swordsCount = TestHelper.countItemsInChest(helper, swordsBottom, Items.DIAMOND_SWORD) +
                          TestHelper.countItemsInChest(helper, swordsBottom, Items.IRON_SWORD);
        if (swordsCount != 2) {
            helper.fail(Component.literal("Expected 2 swords but found " + swordsCount));
            return;
        }

        // Verify pickaxes went to pickaxes stack
        int pickaxesCount = TestHelper.countItemsInChest(helper, pickaxesBottom, Items.DIAMOND_PICKAXE) +
                            TestHelper.countItemsInChest(helper, pickaxesBottom, Items.IRON_PICKAXE);
        if (pickaxesCount != 2) {
            helper.fail(Component.literal("Expected 2 pickaxes but found " + pickaxesCount));
            return;
        }

        helper.succeed();
    }

    /**
     * Test storage with double chests in a row.
     */
    @GameTest
    public void storageWithDoubleChests(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.STORAGE);

        BlockPos inputPos = new BlockPos(1, 1, 1);

        // Create double chest input
        TestHelper.placeDoubleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);

        // Create double chest category
        BlockPos categoryPos = new BlockPos(4, 1, 1);
        TestHelper.placeDoubleChest(helper, categoryPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, categoryPos, Direction.NORTH, "cobblestone");

        // Fill input with cobblestone
        TestHelper.insertItems(helper, inputPos, new ItemStack(Items.COBBLESTONE, 64));

        // Execute sort
        TestHelper.executeSort(helper, inputPos, 10);

        // Verify cobblestone was sorted
        int cobbleCount = TestHelper.countItemsInChest(helper, categoryPos, Items.COBBLESTONE);
        if (cobbleCount != 64) {
            helper.fail(Component.literal("Expected 64 cobblestone but found " + cobbleCount));
            return;
        }

        helper.succeed();
    }

    /**
     * Test storage with categories facing different directions.
     */
    @GameTest
    public void categoriesFacingDifferentDirections(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.STORAGE);

        BlockPos inputPos = new BlockPos(5, 1, 5);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);

        // Category facing north
        BlockPos northPos = new BlockPos(5, 1, 3);
        TestHelper.placeSingleChest(helper, northPos, Direction.SOUTH);
        TestHelper.placeCategorySign(helper, northPos, Direction.SOUTH, "swords");

        // Category facing east
        BlockPos eastPos = new BlockPos(7, 1, 5);
        TestHelper.placeSingleChest(helper, eastPos, Direction.WEST);
        TestHelper.placeCategorySign(helper, eastPos, Direction.WEST, "pickaxes");

        // Add items
        TestHelper.insertItems(helper, inputPos,
            new ItemStack(Items.DIAMOND_SWORD),
            new ItemStack(Items.DIAMOND_PICKAXE)
        );

        // Execute sort
        TestHelper.executeSort(helper, inputPos, 10);

        // Verify items went to correct categories
        int swordCount = TestHelper.countItemsInChest(helper, northPos, Items.DIAMOND_SWORD);
        int pickaxeCount = TestHelper.countItemsInChest(helper, eastPos, Items.DIAMOND_PICKAXE);

        if (swordCount != 1) {
            helper.fail(Component.literal("Expected 1 sword in north chest but found " + swordCount));
            return;
        }
        if (pickaxeCount != 1) {
            helper.fail(Component.literal("Expected 1 pickaxe in east chest but found " + pickaxeCount));
            return;
        }

        helper.succeed();
    }

    /**
     * Test that closest category is used when multiple exist.
     */
    @GameTest
    public void closestCategoryUsed(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.STORAGE);

        BlockPos inputPos = new BlockPos(5, 1, 5);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);

        // Near swords category
        BlockPos nearPos = new BlockPos(6, 1, 5);
        TestHelper.placeSingleChest(helper, nearPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, nearPos, Direction.NORTH, "swords");

        // Far swords category
        BlockPos farPos = new BlockPos(10, 1, 5);
        TestHelper.placeSingleChest(helper, farPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, farPos, Direction.NORTH, "swords");

        TestHelper.insertItems(helper, inputPos, new ItemStack(Items.DIAMOND_SWORD));

        // Execute sort
        TestHelper.executeSort(helper, inputPos, 10);

        // Verify sword went to NEAR category (closest)
        int nearCount = TestHelper.countItemsInChest(helper, nearPos, Items.DIAMOND_SWORD);
        int farCount = TestHelper.countItemsInChest(helper, farPos, Items.DIAMOND_SWORD);

        if (nearCount != 1) {
            helper.fail(Component.literal("Expected sword in near chest but found " + nearCount));
            return;
        }
        if (farCount != 0) {
            helper.fail(Component.literal("Expected no sword in far chest but found " + farCount));
            return;
        }

        helper.succeed();
    }

    /**
     * Test input chest in the middle of storage room.
     */
    @GameTest
    public void inputInMiddleOfRoom(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.STORAGE);

        // Input in center
        BlockPos inputPos = new BlockPos(5, 1, 5);
        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);

        // Categories around the input
        BlockPos northPos = new BlockPos(5, 1, 3);
        TestHelper.placeSingleChest(helper, northPos, Direction.SOUTH);
        TestHelper.placeCategorySign(helper, northPos, Direction.SOUTH, "swords");

        BlockPos southPos = new BlockPos(5, 1, 7);
        TestHelper.placeSingleChest(helper, southPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, southPos, Direction.NORTH, "pickaxes");

        BlockPos eastPos = new BlockPos(7, 1, 5);
        TestHelper.placeSingleChest(helper, eastPos, Direction.WEST);
        TestHelper.placeCategorySign(helper, eastPos, Direction.WEST, "cobblestone");

        // Add items
        TestHelper.insertItems(helper, inputPos,
            new ItemStack(Items.DIAMOND_SWORD),
            new ItemStack(Items.DIAMOND_PICKAXE),
            new ItemStack(Items.COBBLESTONE, 64)
        );

        // Execute sort
        TestHelper.executeSort(helper, inputPos, 10);

        // Verify all items sorted correctly
        if (TestHelper.countItemsInChest(helper, northPos, Items.DIAMOND_SWORD) != 1) {
            helper.fail(Component.literal("Sword not in north chest"));
            return;
        }
        if (TestHelper.countItemsInChest(helper, southPos, Items.DIAMOND_PICKAXE) != 1) {
            helper.fail(Component.literal("Pickaxe not in south chest"));
            return;
        }
        if (TestHelper.countItemsInChest(helper, eastPos, Items.COBBLESTONE) != 64) {
            helper.fail(Component.literal("Cobblestone not in east chest"));
            return;
        }

        helper.succeed();
    }
}

