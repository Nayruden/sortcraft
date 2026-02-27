package net.sortcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.sortcraft.audit.OperationStatus;
import net.sortcraft.audit.SortAuditEntry;
import net.sortcraft.sorting.SortingResults;

import java.util.List;

import static net.sortcraft.gametest.TestHelper.movement;

/**
 * GameTests for input chest stack functionality.
 * Verifies that [input] signs work with vertical stacks of chests,
 * sorting items from all chests in the stack (not just the chest attached to the sign).
 */
public class InputStackGameTest {

    // ========== Basic Input Stack Tests ==========

    /**
     * Test that a single input chest (baseline) still works correctly.
     */
    public void singleInputChestSortsItems(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.SWORDS_SHORT);

        // Setup: single input chest with sign, single category chest
        BlockPos inputPos = new BlockPos(1, 1, 1);
        BlockPos categoryPos = new BlockPos(3, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);
        TestHelper.placeSingleChest(helper, categoryPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, categoryPos, Direction.NORTH, "swords");

        // Insert items into the input chest
        TestHelper.insertItems(helper, inputPos,
                new ItemStack(Items.DIAMOND_SWORD, 1),
                new ItemStack(Items.IRON_SWORD, 1)
        );

        // Execute sort
        SortingResults results = TestHelper.executeSort(helper, inputPos);

        if (results.sorted != 2) {
            helper.fail(Component.literal("Expected 2 items sorted but got " + results.sorted));
            return;
        }

        helper.succeed();
    }

    /**
     * Test that items from a 2-chest input stack are all sorted.
     * Sign is on top chest, items distributed across both chests.
     */
    public void twoChestInputStackSortsAllItems(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.SWORDS_SHORT);

        // Setup: 2-chest input stack with sign on top
        BlockPos bottomInputPos = new BlockPos(1, 1, 1);
        BlockPos topInputPos = bottomInputPos.above();
        BlockPos categoryPos = new BlockPos(3, 1, 1);

        // Place input chest stack
        List<BlockPos> inputStack = TestHelper.placeChestStack(helper, bottomInputPos, 2, Direction.NORTH);
        TestHelper.placeInputSign(helper, topInputPos, Direction.NORTH);

        // Place category chest
        TestHelper.placeSingleChest(helper, categoryPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, categoryPos, Direction.NORTH, "swords");

        // Insert items into BOTH input chests
        TestHelper.insertItems(helper, bottomInputPos,
                new ItemStack(Items.DIAMOND_SWORD, 1),
                new ItemStack(Items.DIAMOND_SWORD, 1)
        );
        TestHelper.insertItems(helper, topInputPos,
                new ItemStack(Items.IRON_SWORD, 1),
                new ItemStack(Items.IRON_SWORD, 1)
        );

        // Execute sort from top input position (where sign is)
        SortingResults results = TestHelper.executeSort(helper, topInputPos, 10);

        // Should sort all 4 items (2 from each chest)
        if (results.sorted != 4) {
            helper.fail(Component.literal("Expected 4 items sorted from 2-chest stack but got " + results.sorted));
            return;
        }

        // Verify both input chests are now empty
        List<ItemStack> bottomContents = TestHelper.getChestContents(helper, bottomInputPos);
        List<ItemStack> topContents = TestHelper.getChestContents(helper, topInputPos);

        if (!bottomContents.isEmpty()) {
            helper.fail(Component.literal("Bottom input chest should be empty but has " + bottomContents.size() + " items"));
            return;
        }
        if (!topContents.isEmpty()) {
            helper.fail(Component.literal("Top input chest should be empty but has " + topContents.size() + " items"));
            return;
        }

        helper.succeed();
    }

    /**
     * Test that a 3-chest input stack sorts items from all chests.
     */
    public void threeChestInputStackSortsAllItems(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.COBBLESTONE);

        // Setup: 3-chest input stack
        BlockPos bottomPos = new BlockPos(1, 1, 1);
        List<BlockPos> inputStack = TestHelper.placeChestStack(helper, bottomPos, 3, Direction.NORTH);
        BlockPos topPos = inputStack.get(2);
        TestHelper.placeInputSign(helper, topPos, Direction.NORTH);

        // Setup category chest
        BlockPos categoryPos = new BlockPos(3, 1, 1);
        TestHelper.placeSingleChest(helper, categoryPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, categoryPos, Direction.NORTH, "cobblestone");

        // Insert 64 cobblestone into each of the 3 input chests
        for (BlockPos pos : inputStack) {
            TestHelper.insertItems(helper, pos, new ItemStack(Items.COBBLESTONE, 64));
        }

        // Execute sort
        SortingResults results = TestHelper.executeSort(helper, topPos, 10);

        // Should sort 192 items (64 * 3)
        if (results.sorted != 192) {
            helper.fail(Component.literal("Expected 192 items sorted from 3-chest stack but got " + results.sorted));
            return;
        }

        helper.succeed();
    }

    // ========== Input Stack Boundary Tests ==========

    /**
     * Test that input stack stops at another category sign below.
     * If a [category] sign is on a chest below, that chest should NOT be part of the input stack.
     */
    public void inputStackStopsAtCategorySign(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.STORAGE);

        // Setup: 3 chests stacked vertically
        // Top chest (Y=3) has [input] sign
        // Middle chest (Y=2) is part of input stack
        // Bottom chest (Y=1) has [swords] category sign - should NOT be included in input
        BlockPos bottomPos = new BlockPos(1, 1, 1);
        List<BlockPos> chestStack = TestHelper.placeChestStack(helper, bottomPos, 3, Direction.NORTH);

        // Place [input] sign on top chest
        TestHelper.placeInputSign(helper, chestStack.get(2), Direction.NORTH);
        // Place [swords] category sign on bottom chest - this should stop the input stack
        TestHelper.placeCategorySign(helper, chestStack.get(0), Direction.NORTH, "swords");

        // Setup separate category chest for pickaxes
        BlockPos pickaxesPos = new BlockPos(3, 1, 1);
        TestHelper.placeSingleChest(helper, pickaxesPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, pickaxesPos, Direction.NORTH, "pickaxes");

        // Insert items: pickaxes in top and middle (input stack), swords in bottom (category)
        TestHelper.insertItems(helper, chestStack.get(2), new ItemStack(Items.IRON_PICKAXE, 1));  // top - input
        TestHelper.insertItems(helper, chestStack.get(1), new ItemStack(Items.IRON_PICKAXE, 1));  // middle - input
        TestHelper.insertItems(helper, chestStack.get(0), new ItemStack(Items.DIAMOND_SWORD, 1)); // bottom - category, not input

        // Execute sort from top (input)
        SortingResults results = TestHelper.executeSort(helper, chestStack.get(2), 10);

        // Should only sort 2 pickaxes (from top and middle)
        // The sword in the bottom chest should NOT be sorted (it's a category chest, not input)
        if (results.sorted != 2) {
            helper.fail(Component.literal("Expected 2 items sorted (input stack stops at category sign) but got " + results.sorted));
            return;
        }

        // Verify the sword is still in the bottom chest (not sorted)
        List<ItemStack> bottomContents = TestHelper.getChestContents(helper, chestStack.get(0));
        if (bottomContents.isEmpty()) {
            helper.fail(Component.literal("Bottom chest (category) should still have the sword"));
            return;
        }

        helper.succeed();
    }

    /**
     * Test that input stack collects items in bottom-to-top order.
     * Items from bottom chest should be processed first.
     */
    public void inputStackProcessesBottomFirst(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.SWORDS_SHORT);

        // Setup: 2-chest input stack and a small category chest
        BlockPos bottomInputPos = new BlockPos(1, 1, 1);
        BlockPos topInputPos = bottomInputPos.above();

        List<BlockPos> inputStack = TestHelper.placeChestStack(helper, bottomInputPos, 2, Direction.NORTH);
        TestHelper.placeInputSign(helper, topInputPos, Direction.NORTH);

        // Category chest - small, will overflow
        BlockPos categoryPos = new BlockPos(3, 1, 1);
        TestHelper.placeSingleChest(helper, categoryPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, categoryPos, Direction.NORTH, "swords");

        // Fill category chest almost full (leave 1 slot)
        for (int i = 0; i < 26; i++) {
            TestHelper.insertItemAt(helper, categoryPos, i, new ItemStack(Items.DIAMOND_SWORD, 1));
        }

        // Insert 1 diamond sword in bottom input, 1 iron sword in top input
        TestHelper.insertItems(helper, bottomInputPos, new ItemStack(Items.DIAMOND_SWORD, 1));
        TestHelper.insertItems(helper, topInputPos, new ItemStack(Items.IRON_SWORD, 1));

        // Execute sort - only 1 item can fit
        SortingResults results = TestHelper.executeSort(helper, topInputPos, 10);

        // Should sort 1 item (the one from bottom chest, processed first)
        if (results.sorted != 1) {
            helper.fail(Component.literal("Expected 1 item sorted (category nearly full) but got " + results.sorted));
            return;
        }

        // Bottom chest should be empty (its item was sorted first)
        List<ItemStack> bottomContents = TestHelper.getChestContents(helper, bottomInputPos);
        if (!bottomContents.isEmpty()) {
            helper.fail(Component.literal("Bottom input chest should be empty (processed first) but has items"));
            return;
        }

        // Top chest should still have the iron sword (overflow)
        List<ItemStack> topContents = TestHelper.getChestContents(helper, topInputPos);
        if (topContents.isEmpty()) {
            helper.fail(Component.literal("Top input chest should still have overflow item"));
            return;
        }

        helper.succeed();
    }

    // ========== Input Stack Audit Tests ==========

    /**
     * Test that audit correctly logs items sorted from a 2-chest input stack.
     */
    public void auditRecordsInputStackSort(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.SWORDS_SHORT);

        // Setup: 2-chest input stack
        BlockPos bottomInputPos = new BlockPos(1, 1, 1);
        BlockPos topInputPos = bottomInputPos.above();

        TestHelper.placeChestStack(helper, bottomInputPos, 2, Direction.NORTH);
        TestHelper.placeInputSign(helper, topInputPos, Direction.NORTH);

        // Setup category chest
        BlockPos categoryPos = new BlockPos(3, 1, 1);
        TestHelper.placeSingleChest(helper, categoryPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, categoryPos, Direction.NORTH, "swords");

        // Insert items into both input chests
        TestHelper.insertItems(helper, bottomInputPos,
                new ItemStack(Items.DIAMOND_SWORD, 1),
                new ItemStack(Items.DIAMOND_SWORD, 1)
        );
        TestHelper.insertItems(helper, topInputPos,
                new ItemStack(Items.IRON_SWORD, 1),
                new ItemStack(Items.IRON_SWORD, 1)
        );

        // Execute sort with audit
        TestHelper.AuditedSortResult result = TestHelper.executeSortWithAudit(helper, topInputPos, 10);
        SortAuditEntry entry = result.auditEntry();

        // Verify success
        if (entry.status() != OperationStatus.SUCCESS) {
            helper.fail(Component.literal("Expected SUCCESS status but got " + entry.status()));
            return;
        }

        // Verify total sorted: 2 diamond swords + 2 iron swords = 4
        if (entry.totalItemsSorted() != 4) {
            helper.fail(Component.literal("Expected 4 items sorted but got " + entry.totalItemsSorted()));
            return;
        }

        // Verify movements contain items from both chests
        BlockPos absCategoryPos = helper.absolutePos(categoryPos);
        TestHelper.assertMovementsExist(helper, entry,
                movement("minecraft:diamond_sword", 2, "swords", absCategoryPos),
                movement("minecraft:iron_sword", 2, "swords", absCategoryPos)
        );

        // Validate audit detail levels
        TestHelper.validateAuditDetailLevels(helper, entry, true, true);

        helper.succeed();
    }

    /**
     * Test that audit correctly logs partial success when some items from input stack are unsortable.
     */
    public void auditRecordsPartialSuccessFromInputStack(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.SWORDS_SHORT);

        // Setup: 2-chest input stack
        BlockPos bottomInputPos = new BlockPos(1, 1, 1);
        BlockPos topInputPos = bottomInputPos.above();

        TestHelper.placeChestStack(helper, bottomInputPos, 2, Direction.NORTH);
        TestHelper.placeInputSign(helper, topInputPos, Direction.NORTH);

        // Setup category chest
        BlockPos categoryPos = new BlockPos(3, 1, 1);
        TestHelper.placeSingleChest(helper, categoryPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, categoryPos, Direction.NORTH, "swords");

        // Insert sortable items in bottom, unsortable in top
        TestHelper.insertItems(helper, bottomInputPos,
                new ItemStack(Items.DIAMOND_SWORD, 1),
                new ItemStack(Items.DIAMOND_SWORD, 1)
        );
        TestHelper.insertItems(helper, topInputPos,
                new ItemStack(Items.DEBUG_STICK, 1),  // unsortable
                new ItemStack(Items.ENDER_PEARL, 5)   // unsortable
        );

        // Execute sort with audit
        TestHelper.AuditedSortResult result = TestHelper.executeSortWithAudit(helper, topInputPos, 10);
        SortAuditEntry entry = result.auditEntry();

        // Verify partial success (some items sorted, some unknown)
        if (entry.status() != OperationStatus.PARTIAL_SUCCESS) {
            helper.fail(Component.literal("Expected PARTIAL_SUCCESS status but got " + entry.status()));
            return;
        }

        // Verify only swords were sorted
        if (entry.totalItemsSorted() != 2) {
            helper.fail(Component.literal("Expected 2 items sorted but got " + entry.totalItemsSorted()));
            return;
        }

        // Verify unknown items from top chest
        TestHelper.assertUnknownItemsContain(helper, entry,
                "minecraft:debug_stick", "minecraft:ender_pearl");

        helper.succeed();
    }

    /**
     * Test that audit correctly logs a 3-chest input stack with mixed item types.
     */
    public void auditRecordsThreeChestInputStackSort(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.STORAGE);

        // Setup: 3-chest input stack
        BlockPos bottomPos = new BlockPos(1, 1, 1);
        List<BlockPos> inputStack = TestHelper.placeChestStack(helper, bottomPos, 3, Direction.NORTH);
        BlockPos topPos = inputStack.get(2);
        TestHelper.placeInputSign(helper, topPos, Direction.NORTH);

        // Setup category chests only (not input - we're using our own input stack)
        var positions = TestScenarios.categoriesOnly(helper, "swords", "pickaxes", "cobblestone");

        // Insert items distributed across all 3 input chests
        TestHelper.insertItems(helper, inputStack.get(0),  // bottom
                new ItemStack(Items.COBBLESTONE, 64)
        );
        TestHelper.insertItems(helper, inputStack.get(1),  // middle
                new ItemStack(Items.IRON_PICKAXE, 1),
                new ItemStack(Items.IRON_PICKAXE, 1)
        );
        TestHelper.insertItems(helper, inputStack.get(2),  // top
                new ItemStack(Items.DIAMOND_SWORD, 1),
                new ItemStack(Items.IRON_SWORD, 1)
        );

        // Execute sort with audit from our input stack
        TestHelper.AuditedSortResult result = TestHelper.executeSortWithAudit(helper, topPos, 15);
        SortAuditEntry entry = result.auditEntry();

        // Verify success
        if (entry.status() != OperationStatus.SUCCESS) {
            helper.fail(Component.literal("Expected SUCCESS status but got " + entry.status()));
            return;
        }

        // Verify total sorted: 64 cobblestone + 2 pickaxes + 2 swords = 68
        if (entry.totalItemsSorted() != 68) {
            helper.fail(Component.literal("Expected 68 items sorted but got " + entry.totalItemsSorted()));
            return;
        }

        // Verify movements from all three chests
        BlockPos absSwordsPos = helper.absolutePos(positions.get("swords"));
        BlockPos absPickaxesPos = helper.absolutePos(positions.get("pickaxes"));
        BlockPos absCobblestonePos = helper.absolutePos(positions.get("cobblestone"));

        TestHelper.assertMovementsExist(helper, entry,
                movement("minecraft:cobblestone", 64, "cobblestone", absCobblestonePos),
                movement("minecraft:iron_pickaxe", 2, "pickaxes", absPickaxesPos),
                movement("minecraft:diamond_sword", 1, "swords", absSwordsPos),
                movement("minecraft:iron_sword", 1, "swords", absSwordsPos)
        );

        helper.succeed();
    }
}

