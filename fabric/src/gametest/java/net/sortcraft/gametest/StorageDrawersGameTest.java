package net.sortcraft.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.sortcraft.audit.OperationStatus;
import net.sortcraft.audit.SortAuditEntry;
import net.sortcraft.sorting.SortingResults;

/**
 * GameTests for Storage Drawers mod integration.
 * Verifies SortCraft correctly sorts items to/from Storage Drawers containers
 * via the standard Container interface.
 *
 * <p>These tests are self-contained and do not modify the existing test infrastructure.
 * All drawer-specific helpers are in {@link StorageDrawersHelper}.
 */
public class StorageDrawersGameTest {

    // ========== Basic Sorting Tests ==========

    /**
     * Sort cobblestone from a chest input to a 1-slot drawer category.
     */
    @GameTest
    public void chestInputToDrawer1Category(GameTestHelper helper) {
        if (!StorageDrawersHelper.requireAvailable(helper)) return;
        TestHelper.setupCategories(StorageDrawersHelper.COBBLESTONE_CATEGORY);

        SortingTestSetup setup = StorageDrawersHelper.chestInputDrawer1Category(helper, "cobblestone");
        TestHelper.insertItems(helper, setup.inputPos(),
                new ItemStack(Items.COBBLESTONE, 64));

        SortingResults results = TestHelper.executeSort(helper, setup.inputPos());
        if (results.sorted != 64) {
            helper.fail(Component.literal("Expected 64 sorted but got " + results.sorted));
            return;
        }

        TestHelper.assertChestContains(helper, setup.categoryPos(), Items.COBBLESTONE, 64);
        TestHelper.assertChestEmpty(helper, setup.inputPos());
        helper.succeed();
    }

    /**
     * Sort two different item types from chest to a 2-slot drawer category.
     */
    @GameTest
    public void chestInputToDrawer2Category(GameTestHelper helper) {
        if (!StorageDrawersHelper.requireAvailable(helper)) return;
        TestHelper.setupCategories(StorageDrawersHelper.BUILDING_CATEGORY);

        SortingTestSetup setup = StorageDrawersHelper.chestInputDrawer2Category(helper, "building");
        TestHelper.insertItems(helper, setup.inputPos(),
                new ItemStack(Items.COBBLESTONE, 32),
                new ItemStack(Items.OAK_PLANKS, 32));

        SortingResults results = TestHelper.executeSort(helper, setup.inputPos());
        if (results.sorted != 64) {
            helper.fail(Component.literal("Expected 64 sorted but got " + results.sorted));
            return;
        }

        TestHelper.assertChestEmpty(helper, setup.inputPos());
        helper.succeed();
    }

    /**
     * Sort four different item types from chest to a 4-slot drawer category.
     */
    @GameTest
    public void chestInputToDrawer4Category(GameTestHelper helper) {
        if (!StorageDrawersHelper.requireAvailable(helper)) return;
        TestHelper.setupCategories(StorageDrawersHelper.BUILDING_CATEGORY);

        SortingTestSetup setup = StorageDrawersHelper.chestInputDrawer4Category(helper, "building");
        TestHelper.insertItems(helper, setup.inputPos(),
                new ItemStack(Items.COBBLESTONE, 16),
                new ItemStack(Items.OAK_PLANKS, 16),
                new ItemStack(Items.STONE_BRICKS, 16),
                new ItemStack(Items.GLASS, 16));

        SortingResults results = TestHelper.executeSort(helper, setup.inputPos());
        if (results.sorted != 64) {
            helper.fail(Component.literal("Expected 64 sorted but got " + results.sorted));
            return;
        }

        TestHelper.assertChestEmpty(helper, setup.inputPos());
        helper.succeed();
    }

    /**
     * Sort items from a 1-slot drawer input to a chest category.
     */
    @GameTest
    public void drawerInputToChestCategory(GameTestHelper helper) {
        if (!StorageDrawersHelper.requireAvailable(helper)) return;
        TestHelper.setupCategories(StorageDrawersHelper.COBBLESTONE_CATEGORY);

        SortingTestSetup setup = StorageDrawersHelper.drawer1InputChestCategory(helper, "cobblestone");
        TestHelper.insertItems(helper, setup.inputPos(),
                new ItemStack(Items.COBBLESTONE, 64));

        SortingResults results = TestHelper.executeSort(helper, setup.inputPos());
        if (results.sorted != 64) {
            helper.fail(Component.literal("Expected 64 sorted but got " + results.sorted));
            return;
        }

        TestHelper.assertChestContains(helper, setup.categoryPos(), Items.COBBLESTONE, 64);
        helper.succeed();
    }

    /**
     * Sort items from a 1-slot drawer input to a 1-slot drawer category.
     */
    @GameTest
    public void drawerInputToDrawerCategory(GameTestHelper helper) {
        if (!StorageDrawersHelper.requireAvailable(helper)) return;
        TestHelper.setupCategories(StorageDrawersHelper.COBBLESTONE_CATEGORY);

        SortingTestSetup setup = StorageDrawersHelper.drawerInputDrawerCategory(helper, "cobblestone");
        TestHelper.insertItems(helper, setup.inputPos(),
                new ItemStack(Items.COBBLESTONE, 64));

        SortingResults results = TestHelper.executeSort(helper, setup.inputPos());
        if (results.sorted != 64) {
            helper.fail(Component.literal("Expected 64 sorted but got " + results.sorted));
            return;
        }

        TestHelper.assertChestContains(helper, setup.categoryPos(), Items.COBBLESTONE, 64);
        TestHelper.assertChestEmpty(helper, setup.inputPos());
        helper.succeed();
    }



    // ========== Half-Depth Drawer Tests ==========

    /**
     * Sort items to a half-depth 1-slot drawer.
     */
    @GameTest
    public void halfDepthDrawer1Category(GameTestHelper helper) {
        if (!StorageDrawersHelper.requireAvailable(helper)) return;
        TestHelper.setupCategories(StorageDrawersHelper.COBBLESTONE_CATEGORY);

        SortingTestSetup setup = StorageDrawersHelper.chestInputHalfDrawerCategory(helper, "cobblestone", 1);
        TestHelper.insertItems(helper, setup.inputPos(),
                new ItemStack(Items.COBBLESTONE, 64));

        SortingResults results = TestHelper.executeSort(helper, setup.inputPos());
        if (results.sorted != 64) {
            helper.fail(Component.literal("Expected 64 sorted but got " + results.sorted));
            return;
        }

        TestHelper.assertChestContains(helper, setup.categoryPos(), Items.COBBLESTONE, 64);
        helper.succeed();
    }

    /**
     * Sort two item types to a half-depth 2-slot drawer.
     */
    @GameTest
    public void halfDepthDrawer2Category(GameTestHelper helper) {
        if (!StorageDrawersHelper.requireAvailable(helper)) return;
        TestHelper.setupCategories(StorageDrawersHelper.BUILDING_CATEGORY);

        SortingTestSetup setup = StorageDrawersHelper.chestInputHalfDrawerCategory(helper, "building", 2);
        TestHelper.insertItems(helper, setup.inputPos(),
                new ItemStack(Items.COBBLESTONE, 32),
                new ItemStack(Items.OAK_PLANKS, 32));

        SortingResults results = TestHelper.executeSort(helper, setup.inputPos());
        if (results.sorted != 64) {
            helper.fail(Component.literal("Expected 64 sorted but got " + results.sorted));
            return;
        }

        TestHelper.assertChestEmpty(helper, setup.inputPos());
        helper.succeed();
    }

    /**
     * Sort four item types to a half-depth 4-slot drawer.
     */
    @GameTest
    public void halfDepthDrawer4Category(GameTestHelper helper) {
        if (!StorageDrawersHelper.requireAvailable(helper)) return;
        TestHelper.setupCategories(StorageDrawersHelper.BUILDING_CATEGORY);

        SortingTestSetup setup = StorageDrawersHelper.chestInputHalfDrawerCategory(helper, "building", 4);
        TestHelper.insertItems(helper, setup.inputPos(),
                new ItemStack(Items.COBBLESTONE, 16),
                new ItemStack(Items.OAK_PLANKS, 16),
                new ItemStack(Items.STONE_BRICKS, 16),
                new ItemStack(Items.GLASS, 16));

        SortingResults results = TestHelper.executeSort(helper, setup.inputPos());
        if (results.sorted != 64) {
            helper.fail(Component.literal("Expected 64 sorted but got " + results.sorted));
            return;
        }

        TestHelper.assertChestEmpty(helper, setup.inputPos());
        helper.succeed();
    }

    // ========== Vertical Stack Tests ==========

    /**
     * Sort items to a vertical stack of 1-slot drawers (2 high).
     */
    @GameTest
    public void verticalDrawerStack(GameTestHelper helper) {
        if (!StorageDrawersHelper.requireAvailable(helper)) return;
        TestHelper.setupCategories(StorageDrawersHelper.COBBLESTONE_CATEGORY);

        SortingTestSetup setup = StorageDrawersHelper.chestInputDrawerStackCategory(
                helper, "cobblestone", 2, StorageDrawersHelper.OAK_FULL_DRAWERS_1);
        TestHelper.insertItems(helper, setup.inputPos(),
                new ItemStack(Items.COBBLESTONE, 64));

        SortingResults results = TestHelper.executeSort(helper, setup.inputPos());
        if (results.sorted != 64) {
            helper.fail(Component.literal("Expected 64 sorted but got " + results.sorted));
            return;
        }

        TestHelper.assertChestEmpty(helper, setup.inputPos());
        helper.succeed();
    }

    // ========== Mixed Container Tests ==========

    /**
     * Sort items to two different categories: one drawer and one chest.
     */
    @GameTest
    public void mixedDrawerAndChestCategories(GameTestHelper helper) {
        if (!StorageDrawersHelper.requireAvailable(helper)) return;
        TestHelper.setupCategories(StorageDrawersHelper.BUILDING_AND_ORES);

        SortingTestSetup setup = StorageDrawersHelper.chestInputDrawerAndChestCategories(
                helper, "building", "ores");
        TestHelper.insertItems(helper, setup.inputPos(),
                new ItemStack(Items.COBBLESTONE, 32),
                new ItemStack(Items.IRON_INGOT, 32));

        SortingResults results = TestHelper.executeSort(helper, setup.inputPos());
        if (results.sorted != 64) {
            helper.fail(Component.literal("Expected 64 sorted but got " + results.sorted));
            return;
        }

        TestHelper.assertChestContains(helper, setup.categoryPos(0), Items.COBBLESTONE, 32);
        TestHelper.assertChestContains(helper, setup.categoryPos(1), Items.IRON_INGOT, 32);
        TestHelper.assertChestEmpty(helper, setup.inputPos());
        helper.succeed();
    }

    // ========== Preview Mode Tests ==========

    /**
     * Preview mode correctly reports items that would sort to a drawer.
     */
    @GameTest
    public void previewModeWithDrawerCategory(GameTestHelper helper) {
        if (!StorageDrawersHelper.requireAvailable(helper)) return;
        TestHelper.setupCategories(StorageDrawersHelper.COBBLESTONE_CATEGORY);

        SortingTestSetup setup = StorageDrawersHelper.chestInputDrawer1Category(helper, "cobblestone");
        TestHelper.insertItems(helper, setup.inputPos(),
                new ItemStack(Items.COBBLESTONE, 64));

        SortingResults results = TestHelper.executeSortPreview(helper, setup.inputPos());
        if (results.sorted != 64) {
            helper.fail(Component.literal("Preview expected 64 sorted but got " + results.sorted));
            return;
        }

        // In preview mode, items should NOT actually move
        int inputCount = TestHelper.countItemsInChest(helper, setup.inputPos(), Items.COBBLESTONE);
        if (inputCount != 64) {
            helper.fail(Component.literal("Preview should not move items; input has " + inputCount));
            return;
        }

        helper.succeed();
    }

    /**
     * Preview mode reports correct category counts for drawer destination.
     */
    @GameTest
    public void previewModeDrawerCategoryCounts(GameTestHelper helper) {
        if (!StorageDrawersHelper.requireAvailable(helper)) return;
        TestHelper.setupCategories(StorageDrawersHelper.BUILDING_CATEGORY);

        SortingTestSetup setup = StorageDrawersHelper.chestInputDrawer2Category(helper, "building");
        TestHelper.insertItems(helper, setup.inputPos(),
                new ItemStack(Items.COBBLESTONE, 32),
                new ItemStack(Items.OAK_PLANKS, 16));

        SortingResults results = TestHelper.executeSortPreview(helper, setup.inputPos());

        // Check category counts
        Integer buildingCount = results.categoryCounts.get("building");
        if (buildingCount == null || buildingCount != 48) {
            helper.fail(Component.literal("Expected 48 in building category but got " + buildingCount));
            return;
        }

        helper.succeed();
    }

    // ========== Audit Logging Tests ==========

    /**
     * Audit correctly records item movements to a drawer category.
     */
    @GameTest
    public void auditRecordsDrawerMovements(GameTestHelper helper) {
        if (!StorageDrawersHelper.requireAvailable(helper)) return;
        TestHelper.setupCategories(StorageDrawersHelper.COBBLESTONE_CATEGORY);

        SortingTestSetup setup = StorageDrawersHelper.chestInputDrawer1Category(helper, "cobblestone");
        TestHelper.insertItems(helper, setup.inputPos(),
                new ItemStack(Items.COBBLESTONE, 64));

        TestHelper.AuditedSortResult result = TestHelper.executeSortWithAudit(helper, setup.inputPos());
        SortAuditEntry entry = result.auditEntry();

        if (entry.status() != OperationStatus.SUCCESS) {
            helper.fail(Component.literal("Expected SUCCESS but got " + entry.status()));
            return;
        }

        if (entry.totalItemsSorted() != 64) {
            helper.fail(Component.literal("Audit expected 64 sorted but got " + entry.totalItemsSorted()));
            return;
        }

        helper.succeed();
    }

    // ========== Edge Case Tests ==========

    /**
     * Items that don't match any category remain in input (drawer as category).
     */
    @GameTest
    public void unsortableItemsRemainInInputWithDrawer(GameTestHelper helper) {
        if (!StorageDrawersHelper.requireAvailable(helper)) return;
        TestHelper.setupCategories(StorageDrawersHelper.COBBLESTONE_CATEGORY);

        SortingTestSetup setup = StorageDrawersHelper.chestInputDrawer1Category(helper, "cobblestone");
        // Insert items that don't match the cobblestone category
        TestHelper.insertItems(helper, setup.inputPos(),
                new ItemStack(Items.DIAMOND, 10));

        SortingResults results = TestHelper.executeSort(helper, setup.inputPos());
        if (results.sorted != 0) {
            helper.fail(Component.literal("Expected 0 sorted but got " + results.sorted));
            return;
        }

        // Diamonds should remain in input
        TestHelper.assertChestContains(helper, setup.inputPos(), Items.DIAMOND, 10);
        helper.succeed();
    }

    /**
     * Mix of sortable and unsortable items with drawer category.
     */
    @GameTest
    public void mixedSortableAndUnsortableWithDrawer(GameTestHelper helper) {
        if (!StorageDrawersHelper.requireAvailable(helper)) return;
        TestHelper.setupCategories(StorageDrawersHelper.COBBLESTONE_CATEGORY);

        SortingTestSetup setup = StorageDrawersHelper.chestInputDrawer1Category(helper, "cobblestone");
        TestHelper.insertItems(helper, setup.inputPos(),
                new ItemStack(Items.COBBLESTONE, 64),
                new ItemStack(Items.DIAMOND, 10));

        SortingResults results = TestHelper.executeSort(helper, setup.inputPos());
        if (results.sorted != 64) {
            helper.fail(Component.literal("Expected 64 sorted but got " + results.sorted));
            return;
        }

        // Cobblestone should be in drawer
        TestHelper.assertChestContains(helper, setup.categoryPos(), Items.COBBLESTONE, 64);
        // Diamonds should remain in input
        TestHelper.assertChestContains(helper, setup.inputPos(), Items.DIAMOND, 10);
        helper.succeed();
    }

    /**
     * Sort to a drawer that already has pre-existing items in it.
     */
    @GameTest
    public void drawerWithExistingItems(GameTestHelper helper) {
        if (!StorageDrawersHelper.requireAvailable(helper)) return;
        TestHelper.setupCategories(StorageDrawersHelper.COBBLESTONE_CATEGORY);

        SortingTestSetup setup = StorageDrawersHelper.chestInputDrawer1Category(helper, "cobblestone");

        // Pre-populate the drawer with some cobblestone
        TestHelper.insertItems(helper, setup.categoryPos(),
                new ItemStack(Items.COBBLESTONE, 32));

        // Now add more to the input
        TestHelper.insertItems(helper, setup.inputPos(),
                new ItemStack(Items.COBBLESTONE, 64));

        SortingResults results = TestHelper.executeSort(helper, setup.inputPos());
        if (results.sorted != 64) {
            helper.fail(Component.literal("Expected 64 sorted but got " + results.sorted));
            return;
        }

        // Drawer should now have 32 + 64 = 96 total cobblestone
        TestHelper.assertChestContains(helper, setup.categoryPos(), Items.COBBLESTONE, 96);
        TestHelper.assertChestEmpty(helper, setup.inputPos());
        helper.succeed();
    }

    /**
     * Sort unstackable items (swords) to a drawer.
     * Each sword occupies one slot since max stack size is 1.
     */
    @GameTest
    public void unstackableItemsInDrawer(GameTestHelper helper) {
        if (!StorageDrawersHelper.requireAvailable(helper)) return;
        TestHelper.setupCategories(StorageDrawersHelper.SWORDS_CATEGORY);

        SortingTestSetup setup = StorageDrawersHelper.chestInputDrawer2Category(helper, "swords");
        TestHelper.insertItems(helper, setup.inputPos(),
                new ItemStack(Items.DIAMOND_SWORD, 1),
                new ItemStack(Items.IRON_SWORD, 1));

        SortingResults results = TestHelper.executeSort(helper, setup.inputPos());
        if (results.sorted != 2) {
            helper.fail(Component.literal("Expected 2 sorted but got " + results.sorted));
            return;
        }

        TestHelper.assertChestEmpty(helper, setup.inputPos());
        helper.succeed();
    }

    /**
     * Sort with no items in input — nothing should happen.
     */
    @GameTest
    public void emptyInputWithDrawerCategory(GameTestHelper helper) {
        if (!StorageDrawersHelper.requireAvailable(helper)) return;
        TestHelper.setupCategories(StorageDrawersHelper.COBBLESTONE_CATEGORY);

        SortingTestSetup setup = StorageDrawersHelper.chestInputDrawer1Category(helper, "cobblestone");
        // Don't insert any items

        SortingResults results = TestHelper.executeSort(helper, setup.inputPos());
        if (results.sorted != 0) {
            helper.fail(Component.literal("Expected 0 sorted but got " + results.sorted));
            return;
        }

        helper.succeed();
    }

    // ========== Compacting Drawer Tests ==========

    /**
     * Sort items to a 2-slot compacting drawer.
     */
    @GameTest
    public void compactingDrawer2Category(GameTestHelper helper) {
        if (!StorageDrawersHelper.requireAvailable(helper)) return;
        TestHelper.setupCategories(StorageDrawersHelper.ORES_CATEGORY);

        SortingTestSetup setup = StorageDrawersHelper.chestInputCompactingDrawerCategory(
                helper, "ores", 2);
        TestHelper.insertItems(helper, setup.inputPos(),
                new ItemStack(Items.IRON_INGOT, 64));

        SortingResults results = TestHelper.executeSort(helper, setup.inputPos());
        if (results.sorted != 64) {
            helper.fail(Component.literal("Expected 64 sorted but got " + results.sorted));
            return;
        }

        TestHelper.assertChestEmpty(helper, setup.inputPos());
        helper.succeed();
    }

    /**
     * Sort items to a 3-slot compacting drawer.
     */
    @GameTest
    public void compactingDrawer3Category(GameTestHelper helper) {
        if (!StorageDrawersHelper.requireAvailable(helper)) return;
        TestHelper.setupCategories(StorageDrawersHelper.ORES_CATEGORY);

        SortingTestSetup setup = StorageDrawersHelper.chestInputCompactingDrawerCategory(
                helper, "ores", 3);
        TestHelper.insertItems(helper, setup.inputPos(),
                new ItemStack(Items.IRON_INGOT, 64));

        SortingResults results = TestHelper.executeSort(helper, setup.inputPos());
        if (results.sorted != 64) {
            helper.fail(Component.literal("Expected 64 sorted but got " + results.sorted));
            return;
        }

        TestHelper.assertChestEmpty(helper, setup.inputPos());
        helper.succeed();
    }

    /**
     * Sort multiple stacks of cobblestone to a drawer to verify
     * the drawer accepts more than one stack per slot (up to 32 stacks capacity).
     */
    @GameTest
    public void multipleStacksToSingleDrawerSlot(GameTestHelper helper) {
        if (!StorageDrawersHelper.requireAvailable(helper)) return;
        TestHelper.setupCategories(StorageDrawersHelper.COBBLESTONE_CATEGORY);

        SortingTestSetup setup = StorageDrawersHelper.chestInputDrawer1Category(helper, "cobblestone");
        // Insert 3 stacks of 64 = 192 total
        TestHelper.insertItems(helper, setup.inputPos(),
                new ItemStack(Items.COBBLESTONE, 64),
                new ItemStack(Items.COBBLESTONE, 64),
                new ItemStack(Items.COBBLESTONE, 64));

        SortingResults results = TestHelper.executeSort(helper, setup.inputPos());
        if (results.sorted != 192) {
            helper.fail(Component.literal("Expected 192 sorted but got " + results.sorted));
            return;
        }

        TestHelper.assertChestContains(helper, setup.categoryPos(), Items.COBBLESTONE, 192);
        TestHelper.assertChestEmpty(helper, setup.inputPos());
        helper.succeed();
    }

    /**
     * Drawer as input with multiple item stacks — sort all to chest category.
     */
    @GameTest
    public void drawerInputMultipleStacks(GameTestHelper helper) {
        if (!StorageDrawersHelper.requireAvailable(helper)) return;
        TestHelper.setupCategories(StorageDrawersHelper.BUILDING_CATEGORY);

        SortingTestSetup setup = StorageDrawersHelper.drawer1InputChestCategory(helper, "building");
        TestHelper.insertItems(helper, setup.inputPos(),
                new ItemStack(Items.COBBLESTONE, 64));

        SortingResults results = TestHelper.executeSort(helper, setup.inputPos());
        if (results.sorted != 64) {
            helper.fail(Component.literal("Expected 64 sorted but got " + results.sorted));
            return;
        }

        TestHelper.assertChestContains(helper, setup.categoryPos(), Items.COBBLESTONE, 64);
        helper.succeed();
    }
}
