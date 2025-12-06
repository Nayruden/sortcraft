package net.sortcraft.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.sortcraft.audit.OperationStatus;
import net.sortcraft.audit.SortAuditEntry;

import static net.sortcraft.gametest.TestHelper.movement;

/**
 * Game tests for the audit logging system.
 * Each test validates audit entries at all three detail levels (FULL, SUMMARY, MINIMAL).
 */
public class AuditGameTest {

    // ========== Test 1: Variety of Items Sorted Successfully ==========

    /**
     * Verifies audit correctly tracks sorting a diverse mix of items to multiple categories.
     * Setup: swords, pickaxes, cobblestone categories with various items
     * Expects: SUCCESS status, correct counts, all detail levels valid
     */
    @GameTest
    public void auditRecordsVarietyOfItemsSorted(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.STORAGE);

        // Setup: input chest with category chests for swords, pickaxes, cobblestone
        var positions = TestScenarios.multiCategory(helper, "swords", "pickaxes", "cobblestone");
        BlockPos inputPos = positions.get("input");

        // Insert variety of items: 5 diamond swords, 3 iron pickaxes, 64 cobblestone, 10 iron swords
        TestHelper.insertItems(helper, inputPos,
                new ItemStack(Items.DIAMOND_SWORD, 1),
                new ItemStack(Items.DIAMOND_SWORD, 1),
                new ItemStack(Items.DIAMOND_SWORD, 1),
                new ItemStack(Items.DIAMOND_SWORD, 1),
                new ItemStack(Items.DIAMOND_SWORD, 1),
                new ItemStack(Items.IRON_PICKAXE, 1),
                new ItemStack(Items.IRON_PICKAXE, 1),
                new ItemStack(Items.IRON_PICKAXE, 1),
                new ItemStack(Items.COBBLESTONE, 64),
                new ItemStack(Items.IRON_SWORD, 1),
                new ItemStack(Items.IRON_SWORD, 1),
                new ItemStack(Items.IRON_SWORD, 1),
                new ItemStack(Items.IRON_SWORD, 1),
                new ItemStack(Items.IRON_SWORD, 1),
                new ItemStack(Items.IRON_SWORD, 1),
                new ItemStack(Items.IRON_SWORD, 1),
                new ItemStack(Items.IRON_SWORD, 1),
                new ItemStack(Items.IRON_SWORD, 1),
                new ItemStack(Items.IRON_SWORD, 1)
        );

        // Execute sort with audit
        TestHelper.AuditedSortResult result = TestHelper.executeSortWithAudit(helper, inputPos, 10);
        SortAuditEntry entry = result.auditEntry();

        // Verify status
        if (entry.status() != OperationStatus.SUCCESS) {
            helper.fail(Component.literal("Expected SUCCESS status but got " + entry.status()));
            return;
        }

        // Verify total sorted: 5 diamond swords + 3 pickaxes + 64 cobblestone + 10 iron swords = 82
        if (entry.totalItemsSorted() != 82) {
            helper.fail(Component.literal("Expected 82 items sorted but got " + entry.totalItemsSorted()));
            return;
        }

        // Verify no issues
        if (!entry.unknownItems().isEmpty()) {
            helper.fail(Component.literal("Expected no unknown items but got: " + entry.unknownItems()));
            return;
        }
        if (!entry.overflowCategories().isEmpty()) {
            helper.fail(Component.literal("Expected no overflow but got: " + entry.overflowCategories()));
            return;
        }

        // Verify movements contain correct items with correct quantities, categories, and positions
        BlockPos swordsPos = helper.absolutePos(positions.get("swords"));
        BlockPos pickaxesPos = helper.absolutePos(positions.get("pickaxes"));
        BlockPos cobblestonePos = helper.absolutePos(positions.get("cobblestone"));

        TestHelper.assertMovementsExist(helper, entry,
                movement("minecraft:diamond_sword", 5, "swords", swordsPos),
                movement("minecraft:iron_sword", 10, "swords", swordsPos),
                movement("minecraft:iron_pickaxe", 3, "pickaxes", pickaxesPos),
                movement("minecraft:cobblestone", 64, "cobblestone", cobblestonePos)
        );

        // Validate all detail levels
        TestHelper.validateAuditDetailLevels(helper, entry, true, true);

        helper.succeed();
    }

    // ========== Test 2: Shulker Containing Bundles ==========

    /**
     * Verifies audit handles deeply nested containers with diverse contents.
     * Setup: shulker containing 4 bundles with various items plus loose items
     * Expects: Correct tracking of all extracted items across categories
     */
    @GameTest
    public void auditRecordsShulkerContainingBundles(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.AUDIT_MULTI);

        var positions = TestScenarios.multiCategory(helper, "swords", "cobblestone", "gems", "containers");
        BlockPos inputPos = positions.get("input");

        // Create bundles with various contents:
        // Bundle 1: 16 cobblestone, 8 sticks (sticks unsortable)
        ItemStack bundle1 = TestHelper.createBundle(
                new ItemStack(Items.COBBLESTONE, 16),
                new ItemStack(Items.STICK, 8)
        );

        // Bundle 2: 5 diamonds, 3 emeralds
        ItemStack bundle2 = TestHelper.createBundle(
                new ItemStack(Items.DIAMOND, 5),
                new ItemStack(Items.EMERALD, 3)
        );

        // Bundle 3: 1 diamond sword (fills the bundle - unstackable)
        ItemStack bundle3 = TestHelper.createBundle(
                new ItemStack(Items.DIAMOND_SWORD, 1)
        );

        // Bundle 4: 10 arrows, 5 bones (stackable unsortables)
        ItemStack bundle4 = TestHelper.createBundle(
                new ItemStack(Items.ARROW, 10),
                new ItemStack(Items.BONE, 5)
        );

        // Create shulker with bundles and loose items
        ItemStack shulker = TestHelper.createShulkerBox(
                bundle1,
                bundle2,
                bundle3,
                bundle4,
                new ItemStack(Items.COBBLESTONE, 32),  // loose
                new ItemStack(Items.IRON_SWORD, 1),    // loose
                new ItemStack(Items.DIAMOND_SWORD, 1)  // loose
        );

        TestHelper.insertItems(helper, inputPos, shulker);

        // Execute sort with audit (larger radius for multi-category)
        TestHelper.AuditedSortResult result = TestHelper.executeSortWithAudit(helper, inputPos, 10);
        SortAuditEntry entry = result.auditEntry();

        // Verify status is PARTIAL_SUCCESS due to unsortable items (sticks, arrows, bones)
        if (entry.status() != OperationStatus.PARTIAL_SUCCESS) {
            helper.fail(Component.literal("Expected PARTIAL_SUCCESS status but got " + entry.status()));
            return;
        }

        // Verify unknown items contains sticks, arrows, bones
        TestHelper.assertUnknownItemsContain(helper, entry,
                "minecraft:stick", "minecraft:arrow", "minecraft:bone");

        // Verify movements for sorted items with full ItemMovementRecord equality
        BlockPos swordsPos = helper.absolutePos(positions.get("swords"));
        BlockPos cobblestonePos = helper.absolutePos(positions.get("cobblestone"));
        BlockPos gemsPos = helper.absolutePos(positions.get("gems"));

        TestHelper.assertMovementsExist(helper, entry,
                movement("minecraft:cobblestone", 48, "cobblestone", cobblestonePos),  // 16 from bundle + 32 loose
                movement("minecraft:diamond", 5, "gems", gemsPos),
                movement("minecraft:emerald", 3, "gems", gemsPos),
                movement("minecraft:diamond_sword", 2, "swords", swordsPos),  // 1 from bundle + 1 loose
                movement("minecraft:iron_sword", 1, "swords", swordsPos)
        );

        // Validate all detail levels
        TestHelper.validateAuditDetailLevels(helper, entry, true, true);

        helper.succeed();
    }

    // ========== Test 3: Unsortable Items ==========

    /**
     * Verifies audit correctly identifies and logs items with no matching category.
     * Setup: swords category only, input contains swords plus items without categories
     * Expects: PARTIAL_SUCCESS, unknownItems populated correctly
     */
    @GameTest
    public void auditRecordsUnsortableItems(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.SWORDS_SHORT);

        SortingTestSetup setup = TestScenarios.inputWithItems(helper, "swords",
                new ItemStack(Items.DIAMOND_SWORD, 1),
                new ItemStack(Items.DIAMOND_SWORD, 1),
                new ItemStack(Items.DIAMOND_SWORD, 1),
                new ItemStack(Items.DIAMOND_SWORD, 1),
                new ItemStack(Items.DIAMOND_SWORD, 1),
                new ItemStack(Items.DEBUG_STICK, 1),
                new ItemStack(Items.DEBUG_STICK, 1),
                new ItemStack(Items.DEBUG_STICK, 1),
                new ItemStack(Items.ENDER_PEARL, 10)
        );

        // Execute sort with audit
        TestHelper.AuditedSortResult result = TestHelper.executeSortWithAudit(helper, setup.inputPos());
        SortAuditEntry entry = result.auditEntry();

        // Verify status
        if (entry.status() != OperationStatus.PARTIAL_SUCCESS) {
            helper.fail(Component.literal("Expected PARTIAL_SUCCESS status but got " + entry.status()));
            return;
        }

        // Verify sorted count (only swords should be sorted)
        if (entry.totalItemsSorted() != 5) {
            helper.fail(Component.literal("Expected 5 items sorted but got " + entry.totalItemsSorted()));
            return;
        }

        // Verify unknown items
        TestHelper.assertUnknownItemsContain(helper, entry,
                "minecraft:debug_stick", "minecraft:ender_pearl");

        // Verify movements for sorted swords with full ItemMovementRecord equality
        BlockPos swordsPos = helper.absolutePos(setup.categoryPos());

        TestHelper.assertMovementsExist(helper, entry,
                movement("minecraft:diamond_sword", 5, "swords", swordsPos)
        );

        // Validate all detail levels
        TestHelper.validateAuditDetailLevels(helper, entry, true, true);

        helper.succeed();
    }

    // ========== Test 4: Overflow to Second Chest ==========

    /**
     * Verifies audit tracks items that overflow from one chest to another in a vertical stack.
     * Setup: 2-chest stack with bottom almost full, input has items that will overflow
     * Expects: Movements show different destination positions
     */
    @GameTest
    public void auditRecordsOverflowToSecondChest(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.COBBLESTONE);

        // Setup: 2-chest stack, bottom has 26 stacks (almost full, 1 slot left)
        SortingTestSetup setup = TestScenarios.inputWithPrefilledCategoryStack(
                helper, "cobblestone",
                ItemQuantity.stacks(Items.COBBLESTONE, 26),  // bottom: 26 stacks
                ItemQuantity.empty(Items.COBBLESTONE)        // top: empty
        );

        // Add 128 cobblestone (2 stacks) - 64 goes to bottom slot, 64 overflows to top
        TestHelper.insertItems(helper, setup.inputPos(),
                new ItemStack(Items.COBBLESTONE, 64),
                new ItemStack(Items.COBBLESTONE, 64)
        );

        // Execute sort with audit
        TestHelper.AuditedSortResult result = TestHelper.executeSortWithAudit(helper, setup.inputPos());
        SortAuditEntry entry = result.auditEntry();

        // Verify success
        if (entry.status() != OperationStatus.SUCCESS) {
            helper.fail(Component.literal("Expected SUCCESS status but got " + entry.status()));
            return;
        }

        // Verify all items sorted
        if (entry.totalItemsSorted() != 128) {
            helper.fail(Component.literal("Expected 128 items sorted but got " + entry.totalItemsSorted()));
            return;
        }

        // Verify no overflow (items went to top chest, not back to input)
        if (!entry.overflowCategories().isEmpty()) {
            helper.fail(Component.literal("Expected no overflow categories but got: " + entry.overflowCategories()));
            return;
        }

        // Validate all detail levels
        String fullJson = TestHelper.validateAuditDetailLevels(helper, entry, true, true);

        // FULL json should have movements
        if (!fullJson.contains("movements")) {
            helper.fail(Component.literal("Expected movements in FULL json"));
            return;
        }

        helper.succeed();
    }

    // ========== Test 5: Category Overflow ==========

    /**
     * Verifies audit tracks when a category runs completely out of space.
     * Setup: Single full category chest, input has items for that category
     * Expects: PARTIAL_SUCCESS, overflowCategories populated
     */
    @GameTest
    public void auditRecordsCategoryOverflow(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.COBBLESTONE);

        // Setup: category chest completely full
        SortingTestSetup setup = TestScenarios.inputWithPrefilledCategory(
                helper, "cobblestone", ItemQuantity.full(Items.COBBLESTONE)
        );

        // Add cobblestone to input
        TestHelper.insertItems(helper, setup.inputPos(), new ItemStack(Items.COBBLESTONE, 64));

        // Execute sort with audit
        TestHelper.AuditedSortResult result = TestHelper.executeSortWithAudit(helper, setup.inputPos());
        SortAuditEntry entry = result.auditEntry();

        // Verify status
        if (entry.status() != OperationStatus.PARTIAL_SUCCESS) {
            helper.fail(Component.literal("Expected PARTIAL_SUCCESS status but got " + entry.status()));
            return;
        }

        // Verify no items were sorted
        if (entry.totalItemsSorted() != 0) {
            helper.fail(Component.literal("Expected 0 items sorted but got " + entry.totalItemsSorted()));
            return;
        }

        // Verify overflow category
        TestHelper.assertOverflowCategoriesContain(helper, entry, "cobblestone");

        // Validate all detail levels (no movements expected since nothing sorted)
        TestHelper.validateAuditDetailLevels(helper, entry, false, false);

        helper.succeed();
    }

    // ========== Test 6: Mixed Overflow and Unsortable ==========

    /**
     * Complex scenario with both overflow AND unsortable items.
     * Setup: swords (full), pickaxes (empty), input has swords, pickaxes, and debug sticks
     * Expects: PARTIAL_SUCCESS, both overflow and unknown items populated
     */
    @GameTest
    public void auditRecordsMixedOverflowAndUnsortable(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.STORAGE);

        // Setup: multi-category with swords full, pickaxes empty
        var positions = TestScenarios.multiCategory(helper, "swords", "pickaxes", "cobblestone");
        BlockPos inputPos = positions.get("input");
        BlockPos swordsPos = positions.get("swords");

        // Fill swords chest completely with diamond swords
        for (int i = 0; i < 27; i++) {
            TestHelper.insertItemAt(helper, swordsPos, i, new ItemStack(Items.DIAMOND_SWORD, 1));
        }

        // Input: 5 diamond swords (overflow), 5 iron pickaxes (sortable), 3 debug sticks (unsortable)
        TestHelper.insertItems(helper, inputPos,
                new ItemStack(Items.DIAMOND_SWORD, 1),
                new ItemStack(Items.DIAMOND_SWORD, 1),
                new ItemStack(Items.DIAMOND_SWORD, 1),
                new ItemStack(Items.DIAMOND_SWORD, 1),
                new ItemStack(Items.DIAMOND_SWORD, 1),
                new ItemStack(Items.IRON_PICKAXE, 1),
                new ItemStack(Items.IRON_PICKAXE, 1),
                new ItemStack(Items.IRON_PICKAXE, 1),
                new ItemStack(Items.IRON_PICKAXE, 1),
                new ItemStack(Items.IRON_PICKAXE, 1),
                new ItemStack(Items.DEBUG_STICK, 1),
                new ItemStack(Items.DEBUG_STICK, 1),
                new ItemStack(Items.DEBUG_STICK, 1)
        );

        // Execute sort with audit
        TestHelper.AuditedSortResult result = TestHelper.executeSortWithAudit(helper, inputPos, 10);
        SortAuditEntry entry = result.auditEntry();

        // Verify status
        if (entry.status() != OperationStatus.PARTIAL_SUCCESS) {
            helper.fail(Component.literal("Expected PARTIAL_SUCCESS status but got " + entry.status()));
            return;
        }

        // Verify only pickaxes were sorted
        if (entry.totalItemsSorted() != 5) {
            helper.fail(Component.literal("Expected 5 items sorted (pickaxes only) but got " + entry.totalItemsSorted()));
            return;
        }

        // Verify overflow category
        TestHelper.assertOverflowCategoriesContain(helper, entry, "swords");

        // Verify unknown items
        TestHelper.assertUnknownItemsContain(helper, entry, "minecraft:debug_stick");

        // Verify movements for sorted pickaxes with full ItemMovementRecord equality
        BlockPos pickaxesPos = helper.absolutePos(positions.get("pickaxes"));

        TestHelper.assertMovementsExist(helper, entry,
                movement("minecraft:iron_pickaxe", 5, "pickaxes", pickaxesPos)
        );

        // Validate all detail levels
        TestHelper.validateAuditDetailLevels(helper, entry, true, true);

        helper.succeed();
    }

    // ========== Test 7: Enchanted Items Metadata ==========

    /**
     * Verifies audit correctly captures enchantment metadata on sorted items.
     * Setup: swords category, input has enchanted diamond sword
     * Expects: Audit entry contains enchantment info in metadata
     */
    @GameTest
    public void auditRecordsEnchantmentMetadata(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.SWORDS_SHORT);

        SortingTestSetup setup = TestScenarios.basicInputAndCategory(helper, "swords");
        BlockPos inputPos = setup.inputPos();

        // Create enchanted sword with Sharpness V
        ItemStack enchantedSword = new ItemStack(Items.DIAMOND_SWORD);
        TestHelper.enchant(helper, enchantedSword, "minecraft:sharpness", 5);

        TestHelper.insertItems(helper, inputPos, enchantedSword);

        // Execute sort with audit
        TestHelper.AuditedSortResult result = TestHelper.executeSortWithAudit(helper, inputPos);
        SortAuditEntry entry = result.auditEntry();

        // Verify success
        if (entry.status() != OperationStatus.SUCCESS) {
            helper.fail(Component.literal("Expected SUCCESS status but got " + entry.status()));
            return;
        }

        // Verify 1 item sorted
        if (entry.totalItemsSorted() != 1) {
            helper.fail(Component.literal("Expected 1 item sorted but got " + entry.totalItemsSorted()));
            return;
        }

        // Verify metadata contains enchantment info in JSON
        String fullJson = TestHelper.validateAuditDetailLevels(helper, entry, true, true);
        if (!fullJson.contains("\"metadata\"")) {
            helper.fail(Component.literal("Expected metadata in audit JSON"));
            return;
        }
        if (!fullJson.contains("\"enchantments\"")) {
            helper.fail(Component.literal("Expected enchantments in metadata"));
            return;
        }
        if (!fullJson.contains("minecraft:sharpness")) {
            helper.fail(Component.literal("Expected sharpness enchantment in metadata"));
            return;
        }
        if (!fullJson.contains("\"level\":5")) {
            helper.fail(Component.literal("Expected level 5 in enchantment metadata"));
            return;
        }

        helper.succeed();
    }

    // ========== Test 8: Custom Named Items Metadata ==========

    /**
     * Verifies audit correctly captures custom name metadata on sorted items.
     * Setup: swords category, input has named diamond sword
     * Expects: Audit entry contains custom name in metadata
     */
    @GameTest
    public void auditRecordsCustomNameMetadata(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.SWORDS_SHORT);

        SortingTestSetup setup = TestScenarios.basicInputAndCategory(helper, "swords");
        BlockPos inputPos = setup.inputPos();

        // Create named sword
        ItemStack namedSword = TestHelper.namedStack(Items.DIAMOND_SWORD, "Excalibur");

        TestHelper.insertItems(helper, inputPos, namedSword);

        // Execute sort with audit
        TestHelper.AuditedSortResult result = TestHelper.executeSortWithAudit(helper, inputPos);
        SortAuditEntry entry = result.auditEntry();

        // Verify success
        if (entry.status() != OperationStatus.SUCCESS) {
            helper.fail(Component.literal("Expected SUCCESS status but got " + entry.status()));
            return;
        }

        // Verify metadata contains custom name in JSON
        String fullJson = TestHelper.validateAuditDetailLevels(helper, entry, true, true);
        if (!fullJson.contains("\"metadata\"")) {
            helper.fail(Component.literal("Expected metadata in audit JSON"));
            return;
        }
        if (!fullJson.contains("\"customName\"")) {
            helper.fail(Component.literal("Expected customName in metadata"));
            return;
        }
        if (!fullJson.contains("Excalibur")) {
            helper.fail(Component.literal("Expected 'Excalibur' in custom name metadata"));
            return;
        }

        helper.succeed();
    }

    // ========== Test 9: Potion Type Metadata ==========

    /**
     * Verifies audit correctly captures potion type metadata on sorted potions.
     * Setup: potions category, input has healing potion
     * Expects: Audit entry contains potion type in metadata
     */
    @GameTest
    public void auditRecordsPotionTypeMetadata(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.POTIONS);

        SortingTestSetup setup = TestScenarios.basicInputAndCategory(helper, "potions");
        BlockPos inputPos = setup.inputPos();

        // Create healing potion
        ItemStack healingPotion = TestHelper.createPotion(helper, "minecraft:healing");

        TestHelper.insertItems(helper, inputPos, healingPotion);

        // Execute sort with audit
        TestHelper.AuditedSortResult result = TestHelper.executeSortWithAudit(helper, inputPos);
        SortAuditEntry entry = result.auditEntry();

        // Verify success
        if (entry.status() != OperationStatus.SUCCESS) {
            helper.fail(Component.literal("Expected SUCCESS status but got " + entry.status()));
            return;
        }

        // Verify metadata contains potion type in JSON
        String fullJson = TestHelper.validateAuditDetailLevels(helper, entry, true, true);
        if (!fullJson.contains("\"metadata\"")) {
            helper.fail(Component.literal("Expected metadata in audit JSON"));
            return;
        }
        if (!fullJson.contains("\"potionType\"")) {
            helper.fail(Component.literal("Expected potionType in metadata"));
            return;
        }
        if (!fullJson.contains("minecraft:healing")) {
            helper.fail(Component.literal("Expected 'minecraft:healing' in potion type metadata"));
            return;
        }

        helper.succeed();
    }

    // ========== Test 10: Multiple Enchantments Metadata ==========

    /**
     * Verifies audit correctly captures multiple enchantments on a single item.
     * Setup: swords category, input has sword with Sharpness V and Unbreaking III
     * Expects: Audit entry contains both enchantments in metadata
     */
    @GameTest
    public void auditRecordsMultipleEnchantments(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.SWORDS_SHORT);

        SortingTestSetup setup = TestScenarios.basicInputAndCategory(helper, "swords");
        BlockPos inputPos = setup.inputPos();

        // Create sword with multiple enchantments
        ItemStack multiEnchantSword = new ItemStack(Items.DIAMOND_SWORD);
        TestHelper.enchant(helper, multiEnchantSword, "minecraft:sharpness", 5);
        TestHelper.enchant(helper, multiEnchantSword, "minecraft:unbreaking", 3);

        TestHelper.insertItems(helper, inputPos, multiEnchantSword);

        // Execute sort with audit
        TestHelper.AuditedSortResult result = TestHelper.executeSortWithAudit(helper, inputPos);
        SortAuditEntry entry = result.auditEntry();

        // Verify success
        if (entry.status() != OperationStatus.SUCCESS) {
            helper.fail(Component.literal("Expected SUCCESS status but got " + entry.status()));
            return;
        }

        // Verify metadata contains both enchantments
        String fullJson = TestHelper.validateAuditDetailLevels(helper, entry, true, true);
        if (!fullJson.contains("minecraft:sharpness")) {
            helper.fail(Component.literal("Expected sharpness enchantment in metadata"));
            return;
        }
        if (!fullJson.contains("minecraft:unbreaking")) {
            helper.fail(Component.literal("Expected unbreaking enchantment in metadata"));
            return;
        }

        helper.succeed();
    }

    // ========== Test 11: Plain Items Have No Metadata ==========

    /**
     * Verifies audit does NOT include metadata for plain items without special properties.
     * Setup: cobblestone category, input has plain cobblestone
     * Expects: Audit entry has no metadata field for plain items
     */
    @GameTest
    public void auditOmitsMetadataForPlainItems(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.COBBLESTONE);

        SortingTestSetup setup = TestScenarios.basicInputAndCategory(helper, "cobblestone");
        BlockPos inputPos = setup.inputPos();

        // Insert plain cobblestone (no enchantments, no name, not a potion)
        TestHelper.insertItems(helper, inputPos, new ItemStack(Items.COBBLESTONE, 64));

        // Execute sort with audit
        TestHelper.AuditedSortResult result = TestHelper.executeSortWithAudit(helper, inputPos);
        SortAuditEntry entry = result.auditEntry();

        // Verify success
        if (entry.status() != OperationStatus.SUCCESS) {
            helper.fail(Component.literal("Expected SUCCESS status but got " + entry.status()));
            return;
        }

        // Verify metadata is NOT present for plain items
        String fullJson = TestHelper.validateAuditDetailLevels(helper, entry, true, true);
        if (fullJson.contains("\"metadata\"")) {
            helper.fail(Component.literal("Expected NO metadata for plain cobblestone, but found metadata in JSON"));
            return;
        }

        helper.succeed();
    }

    // ========== Test 12: Uniform Container Audit ==========

    /**
     * Verifies audit correctly captures uniform container movements.
     * When a shulker box contains 10+ stacks of the same item, it is sorted as a unit
     * to the category of its contents. The audit should record:
     * - The actual container item ID (shulker_box), not the contents
     * - Metadata with uniformContents showing what's inside
     */
    @GameTest
    public void auditRecordsUniformContainerMetadata(GameTestHelper helper) {
        TestHelper.setupCategories(TestCategories.SWORDS_AND_CONTAINERS);

        var positions = TestScenarios.multiCategory(helper, "swords", "containers");
        BlockPos inputPos = positions.get("input");
        BlockPos swordsPos = positions.get("swords");

        // Create shulker with exactly 10 diamond swords (meets uniform threshold)
        // Each stack has 1 sword, total 10 stacks = 10 swords
        ItemStack[] contents = new ItemStack[10];
        for (int i = 0; i < 10; i++) {
            contents[i] = new ItemStack(Items.DIAMOND_SWORD);
        }
        ItemStack shulker = TestHelper.createShulkerBox(contents);

        TestHelper.insertItems(helper, inputPos, shulker);

        // Execute sort with audit
        TestHelper.AuditedSortResult result = TestHelper.executeSortWithAudit(helper, inputPos, 10);
        SortAuditEntry entry = result.auditEntry();

        // Verify success
        if (entry.status() != OperationStatus.SUCCESS) {
            helper.fail(Component.literal("Expected SUCCESS status but got " + entry.status()));
            return;
        }

        // Verify 1 item sorted (the container as a whole)
        if (entry.totalItemsSorted() != 1) {
            helper.fail(Component.literal("Expected 1 item sorted (the shulker) but got " + entry.totalItemsSorted()));
            return;
        }

        // Verify the movement records the CONTAINER item ID, not the contents
        BlockPos absSwordsPos = helper.absolutePos(swordsPos);
        TestHelper.assertMovementsExist(helper, entry,
                movement("minecraft:shulker_box", 1, "swords", absSwordsPos)
        );

        // Verify the JSON contains uniformContents metadata
        String fullJson = TestHelper.validateAuditDetailLevels(helper, entry, true, true);

        // Check for uniformContents structure
        if (!fullJson.contains("\"uniformContents\"")) {
            helper.fail(Component.literal("Expected uniformContents in metadata but not found. JSON: " + fullJson));
            return;
        }

        // Verify the uniform contents has the correct item ID
        if (!fullJson.contains("\"itemId\":\"minecraft:diamond_sword\"")) {
            helper.fail(Component.literal("Expected uniformContents.itemId to be minecraft:diamond_sword. JSON: " + fullJson));
            return;
        }

        // Verify stack count (10 stacks)
        if (!fullJson.contains("\"stackCount\":10")) {
            helper.fail(Component.literal("Expected uniformContents.stackCount to be 10. JSON: " + fullJson));
            return;
        }

        // Verify total item count (10 swords, 1 each)
        if (!fullJson.contains("\"totalItemCount\":10")) {
            helper.fail(Component.literal("Expected uniformContents.totalItemCount to be 10. JSON: " + fullJson));
            return;
        }

        helper.succeed();
    }
}
