package net.sortcraft.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.sortcraft.container.ChestRef;
import net.sortcraft.container.ContainerHelper;

import java.util.List;

/**
 * GameTests for vertical chest stack detection and boundary handling.
 * Tests that stacks are collected DOWNWARD from the sign and that
 * category signs properly separate stacks.
 */
public class VerticalStackGameTest {

    // ========== Basic Stack Detection ==========

    /**
     * Test that a single chest with sign is detected as a stack of 1.
     */
    @GameTest
    public void singleChestStackOfOne(GameTestHelper helper) {
        BlockPos chestPos = new BlockPos(1, 1, 1);
        TestHelper.placeSingleChest(helper, chestPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, chestPos, Direction.NORTH, "test");

        ServerLevel level = helper.getLevel();
        BlockPos absChestPos = helper.absolutePos(chestPos);

        List<ChestRef> stack = ContainerHelper.collectChestStack(level, absChestPos);
        if (stack.size() != 1) {
            helper.fail(Component.literal("Expected stack of 1 but got " + stack.size()));
            return;
        }

        helper.succeed();
    }

    /**
     * Test that 3 chests stacked vertically with sign on top are all collected.
     * Sign on top chest, chests extend downward.
     */
    @GameTest
    public void threeChestStackCollectedDownward(GameTestHelper helper) {
        // Bottom chest at Y=1, middle at Y=2, top at Y=3
        BlockPos bottomPos = new BlockPos(1, 1, 1);
        List<BlockPos> positions = TestHelper.setupCategoryStack(
            helper, bottomPos, 3, Direction.NORTH, Direction.NORTH, "test"
        );

        // Sign is on the top chest (Y=3)
        BlockPos topChest = positions.get(2);

        ServerLevel level = helper.getLevel();
        BlockPos absTopChestPos = helper.absolutePos(topChest);

        List<ChestRef> stack = ContainerHelper.collectChestStack(level, absTopChestPos);
        if (stack.size() != 3) {
            helper.fail(Component.literal("Expected stack of 3 but got " + stack.size()));
            return;
        }

        // Verify order: bottom chest should be first (for bottom-up filling)
        BlockPos firstChestPos = stack.get(0).getPos();
        BlockPos absBottomPos = helper.absolutePos(bottomPos);
        if (!firstChestPos.equals(absBottomPos)) {
            helper.fail(Component.literal("First chest in stack should be bottom chest"));
            return;
        }

        helper.succeed();
    }

    /**
     * Test that 5 chests stacked vertically are all collected.
     */
    @GameTest
    public void fiveChestStackCollected(GameTestHelper helper) {
        BlockPos bottomPos = new BlockPos(1, 1, 1);
        List<BlockPos> positions = TestHelper.setupCategoryStack(
            helper, bottomPos, 5, Direction.NORTH, Direction.NORTH, "test"
        );

        BlockPos topChest = positions.get(4);

        ServerLevel level = helper.getLevel();
        BlockPos absTopChestPos = helper.absolutePos(topChest);

        List<ChestRef> stack = ContainerHelper.collectChestStack(level, absTopChestPos);
        if (stack.size() != 5) {
            helper.fail(Component.literal("Expected stack of 5 but got " + stack.size()));
            return;
        }

        helper.succeed();
    }

    // ========== Stack Boundary Tests ==========

    /**
     * Test that sign on middle chest only collects chests below it.
     * 5 chests total, sign on chest 3 (middle) - should only get 3 chests.
     */
    @GameTest
    public void signOnMiddleChestOnlyCollectsBelow(GameTestHelper helper) {
        // Place 5 chests stacked
        BlockPos bottomPos = new BlockPos(1, 1, 1);
        List<BlockPos> positions = TestHelper.placeChestStack(helper, bottomPos, 5, Direction.NORTH);

        // Put sign on chest 3 (index 2, at Y=3)
        BlockPos middleChest = positions.get(2);
        TestHelper.placeCategorySign(helper, middleChest, Direction.NORTH, "test");

        ServerLevel level = helper.getLevel();
        BlockPos absMiddleChestPos = helper.absolutePos(middleChest);

        List<ChestRef> stack = ContainerHelper.collectChestStack(level, absMiddleChestPos);
        // Should collect: chest at Y=3, Y=2, Y=1 = 3 chests
        if (stack.size() != 3) {
            helper.fail(Component.literal("Expected stack of 3 (middle + below) but got " + stack.size()));
            return;
        }

        helper.succeed();
    }

    /**
     * Test that sign on bottom chest only collects that one chest.
     * Chests above are orphaned (no category).
     */
    @GameTest
    public void signOnBottomChestOnlyThatChest(GameTestHelper helper) {
        // Place 4 chests stacked
        BlockPos bottomPos = new BlockPos(1, 1, 1);
        List<BlockPos> positions = TestHelper.placeChestStack(helper, bottomPos, 4, Direction.NORTH);

        // Put sign on bottom chest only
        TestHelper.placeCategorySign(helper, bottomPos, Direction.NORTH, "test");

        ServerLevel level = helper.getLevel();
        BlockPos absBottomPos = helper.absolutePos(bottomPos);

        List<ChestRef> stack = ContainerHelper.collectChestStack(level, absBottomPos);
        // Should only collect the bottom chest
        if (stack.size() != 1) {
            helper.fail(Component.literal("Expected stack of 1 (bottom only) but got " + stack.size()));
            return;
        }

        helper.succeed();
    }

    // ========== Multi-Category Stack Tests ==========

    /**
     * Test two category signs on same vertical stack.
     * Top sign gets chests down to next sign, bottom sign gets remaining chests.
     */
    @GameTest
    public void twoSignsOnSameStack(GameTestHelper helper) {
        // 5 chests: Y=1,2,3,4,5
        // [swords] sign on chest 5 (top)
        // [pickaxes] sign on chest 2
        // Expected: swords gets chests 5,4,3 (stops at pickaxes sign)
        //           pickaxes gets chests 2,1

        BlockPos bottomPos = new BlockPos(1, 1, 1);
        List<BlockPos> positions = TestHelper.placeChestStack(helper, bottomPos, 5, Direction.NORTH);

        // Sign on top chest (index 4)
        TestHelper.placeCategorySign(helper, positions.get(4), Direction.NORTH, "swords");
        // Sign on chest 2 (index 1)
        TestHelper.placeCategorySign(helper, positions.get(1), Direction.NORTH, "pickaxes");

        ServerLevel level = helper.getLevel();

        // Check swords stack (from top chest)
        BlockPos absSwordsChestPos = helper.absolutePos(positions.get(4));
        List<ChestRef> swordsStack = ContainerHelper.collectChestStack(level, absSwordsChestPos);

        // Check pickaxes stack (from lower chest)
        BlockPos absPickaxesChestPos = helper.absolutePos(positions.get(1));
        List<ChestRef> pickaxesStack = ContainerHelper.collectChestStack(level, absPickaxesChestPos);

        // Swords should have 3 chests (5,4,3 - stops at pickaxes sign on chest 2)
        if (swordsStack.size() != 3) {
            helper.fail(Component.literal("Expected swords stack of 3 but got " + swordsStack.size()));
            return;
        }

        // Pickaxes should have 2 chests (2,1)
        if (pickaxesStack.size() != 2) {
            helper.fail(Component.literal("Expected pickaxes stack of 2 but got " + pickaxesStack.size()));
            return;
        }

        helper.succeed();
    }

    /**
     * Test three category signs creating three separate stacks.
     */
    @GameTest
    public void threeSignsThreeStacks(GameTestHelper helper) {
        // 6 chests: Y=1,2,3,4,5,6
        // [tools] on chest 6
        // [weapons] on chest 4
        // [armor] on chest 2

        BlockPos bottomPos = new BlockPos(1, 1, 1);
        List<BlockPos> positions = TestHelper.placeChestStack(helper, bottomPos, 6, Direction.NORTH);

        TestHelper.placeCategorySign(helper, positions.get(5), Direction.NORTH, "tools");
        TestHelper.placeCategorySign(helper, positions.get(3), Direction.NORTH, "weapons");
        TestHelper.placeCategorySign(helper, positions.get(1), Direction.NORTH, "armor");

        ServerLevel level = helper.getLevel();

        // Tools: chests 6,5 (2 chests)
        BlockPos absToolsChestPos = helper.absolutePos(positions.get(5));
        List<ChestRef> toolsStack = ContainerHelper.collectChestStack(level, absToolsChestPos);

        // Weapons: chests 4,3 (2 chests)
        BlockPos absWeaponsChestPos = helper.absolutePos(positions.get(3));
        List<ChestRef> weaponsStack = ContainerHelper.collectChestStack(level, absWeaponsChestPos);

        // Armor: chests 2,1 (2 chests)
        BlockPos absArmorChestPos = helper.absolutePos(positions.get(1));
        List<ChestRef> armorStack = ContainerHelper.collectChestStack(level, absArmorChestPos);

        if (toolsStack.size() != 2 || weaponsStack.size() != 2 || armorStack.size() != 2) {
            helper.fail(Component.literal("Expected all stacks to have 2 chests. Got: tools=" +
                toolsStack.size() + ", weapons=" + weaponsStack.size() + ", armor=" + armorStack.size()));
            return;
        }

        helper.succeed();
    }

    /**
     * Test adjacent vertical stacks don't interfere with each other.
     */
    @GameTest
    public void adjacentStacksIndependent(GameTestHelper helper) {
        // Two stacks side by side
        // Stack 1: X=1, 3 chests with [swords]
        // Stack 2: X=3, 4 chests with [pickaxes]

        BlockPos stack1Bottom = new BlockPos(1, 1, 1);
        BlockPos stack2Bottom = new BlockPos(3, 1, 1);

        List<BlockPos> stack1 = TestHelper.setupCategoryStack(
            helper, stack1Bottom, 3, Direction.NORTH, Direction.NORTH, "swords");
        List<BlockPos> stack2 = TestHelper.setupCategoryStack(
            helper, stack2Bottom, 4, Direction.NORTH, Direction.NORTH, "pickaxes");

        ServerLevel level = helper.getLevel();

        BlockPos absSwordsChestPos = helper.absolutePos(stack1.get(2));
        List<ChestRef> swordsStack = ContainerHelper.collectChestStack(level, absSwordsChestPos);

        BlockPos absPickaxesChestPos = helper.absolutePos(stack2.get(3));
        List<ChestRef> pickaxesStack = ContainerHelper.collectChestStack(level, absPickaxesChestPos);

        if (swordsStack.size() != 3) {
            helper.fail(Component.literal("Expected swords stack of 3 but got " + swordsStack.size()));
            return;
        }

        if (pickaxesStack.size() != 4) {
            helper.fail(Component.literal("Expected pickaxes stack of 4 but got " + pickaxesStack.size()));
            return;
        }

        helper.succeed();
    }
}

