package net.sortcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.sortcraft.audit.AuditConfig;
import net.sortcraft.audit.SortAuditEntry;
import net.sortcraft.audit.SortAuditLog;
import net.sortcraft.category.CategoryLoader;
import net.sortcraft.category.CategorySet;

import net.sortcraft.container.ChestRef;
import net.sortcraft.container.ContainerHelper;
import net.sortcraft.container.SortContext;
import net.sortcraft.sorting.SortingEngine;
import net.sortcraft.sorting.SortingResults;

import java.util.List;
import java.util.UUID;

/**
 * Game tests for end-to-end sorting with CategorySet override and audit shareId tracking.
 * Validates that the sorting engine correctly uses per-context CategorySets.
 */
public class ShareConfigSortGameTest {

    public void sortWithOverrideCategorySetUsesShareCategories(GameTestHelper helper) {
        // Load global categories: swords only
        TestHelper.setupCategories(TestCategories.SWORDS_SHORT);

        // Create isolated CategorySet: cobblestone only
        CategorySet isolatedSet = CategoryLoader.loadIsolatedFromYaml(TestCategories.COBBLESTONE);

        // Setup: input chest + cobblestone category chest
        var setup = TestScenarios.basicInputAndCategory(helper, "cobblestone");
        TestHelper.insertItems(helper, setup.inputPos(), new ItemStack(Items.COBBLESTONE, 32));

        // Execute sort with the isolated CategorySet
        ServerLevel level = helper.getLevel();
        BlockPos absInputPos = helper.absolutePos(setup.inputPos());
        SortContext context = new SortContext(level, absInputPos, 10);
        context.setCategorySet(isolatedSet);

        List<ChestRef> inputChests = ContainerHelper.collectContainerStack(level, absInputPos);
        SortingResults results = SortingEngine.sortFromContainers(context, level, inputChests, false, null);

        // Cobblestone should be sorted because isolated set has cobblestone category
        if (results.sorted != 32) {
            helper.fail(Component.literal("Expected 32 items sorted with override CategorySet, got " + results.sorted));
        }
        helper.succeed();
    }

    public void sortWithoutOverrideUsesGlobalCategories(GameTestHelper helper) {
        // Load global categories: swords only (no cobblestone)
        TestHelper.setupCategories(TestCategories.SWORDS_SHORT);

        // Setup: input chest + cobblestone category chest
        var setup = TestScenarios.basicInputAndCategory(helper, "cobblestone");
        TestHelper.insertItems(helper, setup.inputPos(), new ItemStack(Items.COBBLESTONE, 32));

        // Execute sort WITHOUT override — should use global (swords only)
        ServerLevel level = helper.getLevel();
        BlockPos absInputPos = helper.absolutePos(setup.inputPos());
        SortContext context = new SortContext(level, absInputPos, 10);
        // No setCategorySet — uses global

        List<ChestRef> inputChests = ContainerHelper.collectContainerStack(level, absInputPos);
        SortingResults results = SortingEngine.sortFromContainers(context, level, inputChests, false, null);

        // Cobblestone should NOT be sorted because global only has swords
        if (results.sorted != 0) {
            helper.fail(Component.literal("Expected 0 items sorted without override (global has no cobblestone), got " + results.sorted));
        }
        if (!results.unknownItems.contains("minecraft:cobblestone")) {
            helper.fail(Component.literal("Expected cobblestone to be unknown with global categories"));
        }
        helper.succeed();
    }

    public void auditEntryRecordsShareIdWhenSet(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.SWORDS_SHORT);

        var setup = TestScenarios.basicInputAndCategory(helper, "swords");
        TestHelper.insertItems(helper, setup.inputPos(), new ItemStack(Items.DIAMOND_SWORD, 1));

        ServerLevel level = helper.getLevel();
        BlockPos absInputPos = helper.absolutePos(setup.inputPos());
        SortContext context = new SortContext(level, absInputPos, 10);

        List<ChestRef> inputChests = ContainerHelper.collectContainerStack(level, absInputPos);

        SortAuditLog audit = SortAuditLog.startForTest(
                "TestPlayer", UUID.fromString("00000000-0000-0000-0000-000000000001"),
                level.dimension().identifier().toString(), absInputPos, 10, false
        );
        audit.setShareId("AbCd1234");

        SortingResults results = SortingEngine.sortFromContainers(context, level, inputChests, false, audit);
        SortAuditEntry entry = audit.complete(results);

        String json = entry.toJson(AuditConfig.DetailLevel.FULL);
        TestHelper.assertJsonContains(helper, json, "shareId");
        if (!json.contains("AbCd1234")) {
            helper.fail(Component.literal("Expected shareId value 'AbCd1234' in JSON"));
        }
        helper.succeed();
    }

    public void auditEntryHasNullShareIdForNormalSort(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.SWORDS_SHORT);

        var setup = TestScenarios.basicInputAndCategory(helper, "swords");
        TestHelper.insertItems(helper, setup.inputPos(), new ItemStack(Items.DIAMOND_SWORD, 1));

        TestHelper.AuditedSortResult result = TestHelper.executeSortWithAudit(helper, setup.inputPos(), 10);
        SortAuditEntry entry = result.auditEntry();

        String json = entry.toJson(AuditConfig.DetailLevel.FULL);
        TestHelper.assertJsonNotContains(helper, json, "shareId");
        helper.succeed();
    }
}

