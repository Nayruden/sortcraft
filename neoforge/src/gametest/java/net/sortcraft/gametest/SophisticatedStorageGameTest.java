package net.sortcraft.gametest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.sortcraft.container.SortCraftStorage;
import net.sortcraft.container.StorageLookup;
import net.sortcraft.audit.OperationStatus;
import net.sortcraft.sorting.SortingResults;

import java.util.Optional;

/**
 * NeoForge GameTests for Sophisticated Storage integration.
 * Uses runtime block registry lookup only (no compile-time mod dependency).
 */
public class SophisticatedStorageGameTest {

    public void chestInputToSophisticatedBarrelCategory(GameTestHelper helper) {
        if (!SophisticatedStorageHelper.requireAvailable(helper, SophisticatedStorageHelper.OAK_BARREL)) return;
        TestHelper.setupCategories(SophisticatedStorageHelper.COBBLESTONE_CATEGORY);

        SortingTestSetup setup = SophisticatedStorageHelper.chestInputSophisticatedBarrelCategory(helper, "cobblestone");
        TestHelper.insertItems(helper, setup.inputPos(), new ItemStack(Items.COBBLESTONE, 64));

        SortingResults results = TestHelper.executeSort(helper, setup.inputPos());
        if (results.sorted != 64) {
            helper.fail(Component.literal("Expected 64 sorted but got " + results.sorted));
            return;
        }

        TestHelper.assertChestContains(helper, setup.categoryPos(), Items.COBBLESTONE, 64);
        TestHelper.assertChestEmpty(helper, setup.inputPos());
        helper.succeed();
    }

    public void sophisticatedBarrelInputToChestCategory(GameTestHelper helper) {
        if (!SophisticatedStorageHelper.requireAvailable(helper, SophisticatedStorageHelper.OAK_BARREL)) return;
        TestHelper.setupCategories(SophisticatedStorageHelper.COBBLESTONE_CATEGORY);

        SortingTestSetup setup = SophisticatedStorageHelper.sophisticatedBarrelInputChestCategory(helper, "cobblestone");
        TestHelper.insertItems(helper, setup.inputPos(), new ItemStack(Items.COBBLESTONE, 64));

        SortingResults results = TestHelper.executeSort(helper, setup.inputPos());
        if (results.sorted != 64) {
            helper.fail(Component.literal("Expected 64 sorted but got " + results.sorted));
            return;
        }

        TestHelper.assertChestContains(helper, setup.categoryPos(), Items.COBBLESTONE, 64);
        TestHelper.assertChestEmpty(helper, setup.inputPos());
        helper.succeed();
    }

    public void previewModeWithSophisticatedStorageCategory(GameTestHelper helper) {
        if (!SophisticatedStorageHelper.requireAvailable(helper, SophisticatedStorageHelper.OAK_CHEST)) return;
        TestHelper.setupCategories(SophisticatedStorageHelper.COBBLESTONE_CATEGORY);

        SortingTestSetup setup = SophisticatedStorageHelper.chestInputSophisticatedChestCategory(helper, "cobblestone");
        TestHelper.insertItems(helper, setup.inputPos(), new ItemStack(Items.COBBLESTONE, 32));

        SortingResults results = TestHelper.executeSortPreview(helper, setup.inputPos());
        if (results.sorted != 32) {
            helper.fail(Component.literal("Preview expected 32 sorted but got " + results.sorted));
            return;
        }

        TestHelper.assertChestContains(helper, setup.inputPos(), Items.COBBLESTONE, 32);
        TestHelper.assertChestEmpty(helper, setup.categoryPos());
        helper.succeed();
    }

    public void auditRecordsSophisticatedStorageMovement(GameTestHelper helper) {
        if (!SophisticatedStorageHelper.requireAvailable(helper, SophisticatedStorageHelper.OAK_BARREL)) return;
        TestHelper.setupCategories(SophisticatedStorageHelper.COBBLESTONE_CATEGORY);

        SortingTestSetup setup = SophisticatedStorageHelper.chestInputSophisticatedBarrelCategory(helper, "cobblestone");
        TestHelper.insertItems(helper, setup.inputPos(), new ItemStack(Items.COBBLESTONE, 16));

        TestHelper.AuditedSortResult audited = TestHelper.executeSortWithAudit(helper, setup.inputPos());
        if (audited.auditEntry().status() != OperationStatus.SUCCESS) {
            helper.fail(Component.literal("Expected SUCCESS but got " + audited.auditEntry().status()));
            return;
        }
        if (audited.auditEntry().totalItemsSorted() != 16) {
            helper.fail(Component.literal("Expected 16 sorted but got " + audited.auditEntry().totalItemsSorted()));
            return;
        }

        helper.succeed();
    }

    public void unsortableItemsRemainInInputWithSophisticatedStorage(GameTestHelper helper) {
        if (!SophisticatedStorageHelper.requireAvailable(helper, SophisticatedStorageHelper.OAK_BARREL)) return;
        TestHelper.setupCategories(SophisticatedStorageHelper.COBBLESTONE_CATEGORY);

        SortingTestSetup setup = SophisticatedStorageHelper.chestInputSophisticatedBarrelCategory(helper, "cobblestone");
        TestHelper.insertItems(helper, setup.inputPos(), new ItemStack(Items.DIAMOND, 10));

        SortingResults results = TestHelper.executeSort(helper, setup.inputPos());
        if (results.sorted != 0) {
            helper.fail(Component.literal("Expected 0 sorted but got " + results.sorted));
            return;
        }

        TestHelper.assertChestContains(helper, setup.inputPos(), Items.DIAMOND, 10);
        TestHelper.assertChestEmpty(helper, setup.categoryPos());
        helper.succeed();
    }

    public void sophisticatedStorageCategoryWithExistingItems(GameTestHelper helper) {
        if (!SophisticatedStorageHelper.requireAvailable(helper, SophisticatedStorageHelper.OAK_BARREL)) return;
        TestHelper.setupCategories(SophisticatedStorageHelper.COBBLESTONE_CATEGORY);

        SortingTestSetup setup = SophisticatedStorageHelper.chestInputSophisticatedBarrelCategory(helper, "cobblestone");
        TestHelper.insertItems(helper, setup.categoryPos(), new ItemStack(Items.COBBLESTONE, 16));
        TestHelper.insertItems(helper, setup.inputPos(), new ItemStack(Items.COBBLESTONE, 48));

        SortingResults results = TestHelper.executeSort(helper, setup.inputPos());
        if (results.sorted != 48) {
            helper.fail(Component.literal("Expected 48 sorted but got " + results.sorted));
            return;
        }

        TestHelper.assertChestContains(helper, setup.categoryPos(), Items.COBBLESTONE, 64);
        TestHelper.assertChestEmpty(helper, setup.inputPos());
        helper.succeed();
    }

    public void verticalSophisticatedBarrelStack(GameTestHelper helper) {
        if (!SophisticatedStorageHelper.requireAvailable(helper, SophisticatedStorageHelper.OAK_BARREL)) return;
        TestHelper.setupCategories(SophisticatedStorageHelper.COBBLESTONE_CATEGORY);

        SortingTestSetup setup =
                SophisticatedStorageHelper.chestInputSophisticatedBarrelStackCategory(helper, "cobblestone", 2);
        TestHelper.insertItems(helper, setup.inputPos(),
                new ItemStack(Items.COBBLESTONE, 64),
                new ItemStack(Items.COBBLESTONE, 64));

        SortingResults results = TestHelper.executeSort(helper, setup.inputPos());
        if (results.sorted != 128) {
            helper.fail(Component.literal("Expected 128 sorted to Sophisticated stack but got " + results.sorted));
            return;
        }

        int total = TestHelper.countItemsInChest(helper, setup.categoryPos(0), Items.COBBLESTONE)
                + TestHelper.countItemsInChest(helper, setup.categoryPos(1), Items.COBBLESTONE);
        if (total != 128) {
            helper.fail(Component.literal("Expected 128 total in stack but got " + total));
            return;
        }

        helper.succeed();
    }

    public void mixedSophisticatedAndChestCategories(GameTestHelper helper) {
        if (!SophisticatedStorageHelper.requireAvailable(helper, SophisticatedStorageHelper.OAK_BARREL)) return;
        TestHelper.setupCategories(SophisticatedStorageHelper.BUILDING_AND_ORES);

        SortingTestSetup setup = SophisticatedStorageHelper
                .chestInputSophisticatedAndChestCategories(helper, "building", "ores");
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

    public void chestInputToSophisticatedChestCategory(GameTestHelper helper) {
        if (!SophisticatedStorageHelper.requireAvailable(helper, SophisticatedStorageHelper.OAK_CHEST)) return;
        TestHelper.setupCategories(SophisticatedStorageHelper.COBBLESTONE_CATEGORY);

        SortingTestSetup setup = SophisticatedStorageHelper.chestInputSophisticatedChestCategory(helper, "cobblestone");
        TestHelper.insertItems(helper, setup.inputPos(), new ItemStack(Items.COBBLESTONE, 64));

        SortingResults results = TestHelper.executeSort(helper, setup.inputPos());
        if (results.sorted != 64) {
            helper.fail(Component.literal("Expected 64 sorted but got " + results.sorted));
            return;
        }

        TestHelper.assertChestContains(helper, setup.categoryPos(), Items.COBBLESTONE, 64);
        TestHelper.assertChestEmpty(helper, setup.inputPos());
        helper.succeed();
    }

    public void unstackableItemsInSophisticatedStorage(GameTestHelper helper) {
        if (!SophisticatedStorageHelper.requireAvailable(helper, SophisticatedStorageHelper.OAK_CHEST)) return;
        TestHelper.setupCategories(SophisticatedStorageHelper.SWORDS_CATEGORY);

        SortingTestSetup setup = SophisticatedStorageHelper.chestInputSophisticatedChestCategory(helper, "swords");
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

    public void fullSophisticatedDestinationLeavesInputUnsorted(GameTestHelper helper) {
        if (!SophisticatedStorageHelper.requireAvailable(helper, SophisticatedStorageHelper.OAK_BARREL)) return;
        TestHelper.setupCategories(SophisticatedStorageHelper.COBBLESTONE_CATEGORY);

        SortingTestSetup setup = SophisticatedStorageHelper.chestInputSophisticatedBarrelCategory(helper, "cobblestone");
        Optional<SortCraftStorage> storageOpt = StorageLookup.getStorageAt(helper.getLevel(), helper.absolutePos(setup.categoryPos()));
        if (storageOpt.isEmpty()) {
            helper.fail(Component.literal("Failed to resolve Sophisticated destination storage"));
            return;
        }

        fillStorageToCapacity(storageOpt.get(), new ItemStack(Items.COBBLESTONE, 64));
        TestHelper.insertItems(helper, setup.inputPos(), new ItemStack(Items.COBBLESTONE, 64));

        SortingResults results = TestHelper.executeSort(helper, setup.inputPos());
        if (results.sorted != 0) {
            helper.fail(Component.literal("Expected 0 sorted with full destination but got " + results.sorted));
            return;
        }
        if (!results.overflowCategories.contains("cobblestone")) {
            helper.fail(Component.literal("Expected overflow category 'cobblestone' but got " + results.overflowCategories));
            return;
        }

        TestHelper.assertChestContains(helper, setup.inputPos(), Items.COBBLESTONE, 64);
        helper.succeed();
    }

    private static void fillStorageToCapacity(SortCraftStorage storage, ItemStack filler) {
        int safety = 0;
        while (safety++ < 10000) {
            int simulated = storage.insert(filler.copy(), true);
            if (simulated <= 0) {
                return;
            }
            ItemStack toInsert = filler.copy();
            toInsert.setCount(simulated);
            int inserted = storage.insert(toInsert, false);
            if (inserted <= 0) {
                return;
            }
        }
        throw new IllegalStateException("Storage did not reach capacity within safety bound");
    }
}
