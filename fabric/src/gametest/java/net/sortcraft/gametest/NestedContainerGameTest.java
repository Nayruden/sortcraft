package net.sortcraft.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.sortcraft.category.CategoryLoader;

import java.util.List;

/**
 * GameTests for nested container handling.
 * Critical tests for structure preservation when sorting fails.
 *
 * Key invariant: If a shulker contains a bundle that contains an unsortable item,
 * all three (shulker, bundle, item) must remain intact in the input chest.
 */
public class NestedContainerGameTest {

    private static final String SWORDS_CATEGORY = """
        swords:
          items:
          - minecraft:diamond_sword
          - minecraft:iron_sword
          - minecraft:stone_sword
          - minecraft:wooden_sword
          - minecraft:golden_sword
          - minecraft:netherite_sword
        """;

    // ========== Bundle Tests ==========

    /**
     * Test that a bundle with sortable items gets its contents sorted.
     */
    @GameTest
    public void bundleWithSortableItemsGetsSorted(GameTestHelper helper) {
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(SWORDS_CATEGORY);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        BlockPos categoryPos = new BlockPos(3, 1, 1);

        // Setup input chest with bundle containing swords
        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);

        // Setup category chest
        TestHelper.placeSingleChest(helper, categoryPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, categoryPos, Direction.NORTH, "swords");

        // Create bundle with 2 diamond swords
        ItemStack bundle = TestHelper.createBundle(
            new ItemStack(Items.DIAMOND_SWORD),
            new ItemStack(Items.DIAMOND_SWORD)
        );
        TestHelper.insertItems(helper, inputPos, bundle);

        // Execute sort
        TestHelper.executeSort(helper, inputPos);

        // Verify: swords should be in category chest
        int swordsInCategory = TestHelper.countItemsInChest(helper, categoryPos, Items.DIAMOND_SWORD);
        if (swordsInCategory != 2) {
            helper.fail(Component.literal("Expected 2 swords in category chest but found " + swordsInCategory));
            return;
        }

        helper.succeed();
    }

    /**
     * Test that a bundle with unsortable items remains intact in input.
     */
    @GameTest
    public void bundleWithUnsortableItemsRemainsIntact(GameTestHelper helper) {
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(SWORDS_CATEGORY);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        BlockPos categoryPos = new BlockPos(3, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);
        TestHelper.placeSingleChest(helper, categoryPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, categoryPos, Direction.NORTH, "swords");

        // Create bundle with unsortable item (debug_stick has no category)
        ItemStack bundle = TestHelper.createBundle(
            new ItemStack(Items.DEBUG_STICK)
        );
        TestHelper.insertItems(helper, inputPos, bundle);

        // Execute sort
        TestHelper.executeSort(helper, inputPos);

        // Verify: bundle should still be in input chest
        List<ItemStack> remaining = TestHelper.getChestContents(helper, inputPos);
        if (remaining.isEmpty()) {
            helper.fail(Component.literal("Bundle with unsortable item should remain in input"));
            return;
        }

        // Verify the bundle still contains the debug stick
        ItemStack remainingBundle = remaining.get(0);
        if (!remainingBundle.is(Items.BUNDLE)) {
            helper.fail(Component.literal("Expected bundle in input but found " + remainingBundle.getItem()));
            return;
        }

        List<ItemStack> bundleContents = TestHelper.getBundleContents(remainingBundle);
        boolean hasDebugStick = bundleContents.stream().anyMatch(s -> s.is(Items.DEBUG_STICK));
        if (!hasDebugStick) {
            helper.fail(Component.literal("Bundle should still contain debug_stick"));
            return;
        }

        helper.succeed();
    }

    // ========== Shulker Box Tests ==========

    /**
     * Test that a shulker with sortable items gets its contents sorted.
     */
    @GameTest
    public void shulkerWithSortableItemsGetsSorted(GameTestHelper helper) {
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(SWORDS_CATEGORY);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        BlockPos categoryPos = new BlockPos(3, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);
        TestHelper.placeSingleChest(helper, categoryPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, categoryPos, Direction.NORTH, "swords");

        // Create shulker with swords
        ItemStack shulker = TestHelper.createShulkerBox(
            new ItemStack(Items.DIAMOND_SWORD),
            new ItemStack(Items.IRON_SWORD)
        );
        TestHelper.insertItems(helper, inputPos, shulker);

        // Execute sort
        TestHelper.executeSort(helper, inputPos);

        // Verify: swords should be in category chest
        int swordsInCategory = TestHelper.countItemsInChest(helper, categoryPos, Items.DIAMOND_SWORD) +
                               TestHelper.countItemsInChest(helper, categoryPos, Items.IRON_SWORD);
        if (swordsInCategory != 2) {
            helper.fail(Component.literal("Expected 2 swords in category chest but found " + swordsInCategory));
            return;
        }

        helper.succeed();
    }

    // ========== Nested Container Tests (Critical) ==========

    /**
     * CRITICAL TEST: Shulker containing bundle with unsortable item.
     * All three (shulker, bundle, item) must remain intact in input chest.
     */
    @GameTest
    public void shulkerBundleUnsortableItemAllRemainIntact(GameTestHelper helper) {
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(SWORDS_CATEGORY);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        BlockPos categoryPos = new BlockPos(3, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);
        TestHelper.placeSingleChest(helper, categoryPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, categoryPos, Direction.NORTH, "swords");

        // Create nested structure: Shulker → Bundle → debug_stick (unsortable)
        ItemStack innerBundle = TestHelper.createBundle(
            new ItemStack(Items.DEBUG_STICK)
        );
        ItemStack shulker = TestHelper.createShulkerBox(innerBundle);
        TestHelper.insertItems(helper, inputPos, shulker);

        // Execute sort
        TestHelper.executeSort(helper, inputPos);

        // Verify: Input chest should still contain exactly 1 item (the shulker)
        List<ItemStack> remaining = TestHelper.getChestContents(helper, inputPos);
        if (remaining.size() != 1) {
            helper.fail(Component.literal("Expected 1 item in input (shulker) but found " + remaining.size()));
            return;
        }

        ItemStack remainingShulker = remaining.get(0);
        if (!remainingShulker.is(Items.SHULKER_BOX)) {
            helper.fail(Component.literal("Expected shulker in input but found " + remainingShulker.getItem()));
            return;
        }

        // Verify: Shulker still contains exactly 1 item (the bundle)
        List<ItemStack> shulkerContents = TestHelper.getShulkerContents(remainingShulker);
        List<ItemStack> nonEmptyShulkerContents = shulkerContents.stream()
            .filter(s -> !s.isEmpty())
            .toList();
        if (nonEmptyShulkerContents.size() != 1) {
            helper.fail(Component.literal("Expected 1 item in shulker (bundle) but found " + nonEmptyShulkerContents.size()));
            return;
        }

        ItemStack remainingBundle = nonEmptyShulkerContents.get(0);
        if (!remainingBundle.is(Items.BUNDLE)) {
            helper.fail(Component.literal("Expected bundle in shulker but found " + remainingBundle.getItem()));
            return;
        }

        // Verify: Bundle still contains exactly 1 item (the debug_stick)
        List<ItemStack> bundleContents = TestHelper.getBundleContents(remainingBundle);
        if (bundleContents.size() != 1) {
            helper.fail(Component.literal("Expected 1 item in bundle but found " + bundleContents.size()));
            return;
        }

        if (!bundleContents.get(0).is(Items.DEBUG_STICK)) {
            helper.fail(Component.literal("Expected debug_stick in bundle but found " + bundleContents.get(0).getItem()));
            return;
        }

        helper.succeed();
    }

    /**
     * Test shulker with bundle containing mixed items (some sortable, some not).
     * Sortable items should be sorted, unsortable should remain in structure.
     */
    @GameTest
    public void shulkerBundleMixedItemsPartialSort(GameTestHelper helper) {
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(SWORDS_CATEGORY);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        BlockPos categoryPos = new BlockPos(3, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);
        TestHelper.placeSingleChest(helper, categoryPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, categoryPos, Direction.NORTH, "swords");

        // Create: Shulker → Bundle → [diamond_sword (sortable), debug_stick (unsortable)]
        ItemStack innerBundle = TestHelper.createBundle(
            new ItemStack(Items.DIAMOND_SWORD),
            new ItemStack(Items.DEBUG_STICK)
        );
        ItemStack shulker = TestHelper.createShulkerBox(innerBundle);
        TestHelper.insertItems(helper, inputPos, shulker);

        // Execute sort
        TestHelper.executeSort(helper, inputPos);

        // Verify: Diamond sword should be in category chest
        int swordsInCategory = TestHelper.countItemsInChest(helper, categoryPos, Items.DIAMOND_SWORD);
        if (swordsInCategory != 1) {
            helper.fail(Component.literal("Expected 1 sword in category chest but found " + swordsInCategory));
            return;
        }

        // Verify: Shulker with bundle containing debug_stick should remain in input
        List<ItemStack> remaining = TestHelper.getChestContents(helper, inputPos);
        boolean hasShulkerWithDebugStick = remaining.stream()
            .filter(s -> s.is(Items.SHULKER_BOX))
            .flatMap(s -> TestHelper.getShulkerContents(s).stream())
            .filter(s -> s.is(Items.BUNDLE))
            .flatMap(b -> TestHelper.getBundleContents(b).stream())
            .anyMatch(s -> s.is(Items.DEBUG_STICK));

        if (!hasShulkerWithDebugStick) {
            helper.fail(Component.literal("Debug stick should remain in bundle inside shulker"));
            return;
        }

        helper.succeed();
    }

    /**
     * Test that empty bundle is handled correctly.
     */
    @GameTest
    public void emptyBundleHandledCorrectly(GameTestHelper helper) {
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(SWORDS_CATEGORY);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        BlockPos categoryPos = new BlockPos(3, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);
        TestHelper.placeSingleChest(helper, categoryPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, categoryPos, Direction.NORTH, "swords");

        // Create empty bundle
        ItemStack emptyBundle = new ItemStack(Items.BUNDLE);
        TestHelper.insertItems(helper, inputPos, emptyBundle);

        // Execute sort
        TestHelper.executeSort(helper, inputPos);

        // Empty bundle should remain in input (no category for bundles)
        List<ItemStack> remaining = TestHelper.getChestContents(helper, inputPos);
        boolean hasBundle = remaining.stream().anyMatch(s -> s.is(Items.BUNDLE));
        if (!hasBundle) {
            helper.fail(Component.literal("Empty bundle should remain in input"));
            return;
        }

        helper.succeed();
    }

    /**
     * Test that empty shulker is handled correctly.
     */
    @GameTest
    public void emptyShulkerHandledCorrectly(GameTestHelper helper) {
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(SWORDS_CATEGORY);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        BlockPos categoryPos = new BlockPos(3, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);
        TestHelper.placeSingleChest(helper, categoryPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, categoryPos, Direction.NORTH, "swords");

        // Create empty shulker
        ItemStack emptyShulker = new ItemStack(Items.SHULKER_BOX);
        TestHelper.insertItems(helper, inputPos, emptyShulker);

        // Execute sort
        TestHelper.executeSort(helper, inputPos);

        // Empty shulker should remain in input (no category for shulkers)
        List<ItemStack> remaining = TestHelper.getChestContents(helper, inputPos);
        boolean hasShulker = remaining.stream().anyMatch(s -> s.is(Items.SHULKER_BOX));
        if (!hasShulker) {
            helper.fail(Component.literal("Empty shulker should remain in input"));
            return;
        }

        helper.succeed();
    }

    /**
     * Test multiple bundles with different contents.
     */
    @GameTest
    public void multipleBundlesDifferentContents(GameTestHelper helper) {
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(SWORDS_CATEGORY);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        BlockPos categoryPos = new BlockPos(3, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);
        TestHelper.placeSingleChest(helper, categoryPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, categoryPos, Direction.NORTH, "swords");

        // Create bundle 1 with sortable items
        ItemStack bundle1 = TestHelper.createBundle(
            new ItemStack(Items.DIAMOND_SWORD)
        );
        // Create bundle 2 with unsortable items
        ItemStack bundle2 = TestHelper.createBundle(
            new ItemStack(Items.DEBUG_STICK)
        );
        TestHelper.insertItems(helper, inputPos, bundle1, bundle2);

        // Execute sort
        TestHelper.executeSort(helper, inputPos);

        // Verify: Sword from bundle1 should be in category
        int swordsInCategory = TestHelper.countItemsInChest(helper, categoryPos, Items.DIAMOND_SWORD);
        if (swordsInCategory != 1) {
            helper.fail(Component.literal("Expected 1 sword in category but found " + swordsInCategory));
            return;
        }

        // Verify: Bundle2 with debug_stick should remain in input
        List<ItemStack> remaining = TestHelper.getChestContents(helper, inputPos);
        boolean hasDebugStickBundle = remaining.stream()
            .filter(s -> s.is(Items.BUNDLE))
            .flatMap(b -> TestHelper.getBundleContents(b).stream())
            .anyMatch(s -> s.is(Items.DEBUG_STICK));

        if (!hasDebugStickBundle) {
            helper.fail(Component.literal("Bundle with debug_stick should remain in input"));
            return;
        }

        helper.succeed();
    }

    // ========== Uniform Container Threshold Tests ==========

    private static final String SWORDS_AND_CONTAINERS_CATEGORY = """
        swords:
          items:
          - minecraft:diamond_sword
          - minecraft:iron_sword
        containers:
          items:
          - minecraft:shulker_box
          - minecraft:bundle
        """;

    /**
     * Test that a shulker with exactly 10 stacks of the same item is sorted as a unit.
     * The uniform container threshold is 10 - meeting it means the container is sorted
     * to the category of its contents, not extracted.
     */
    @GameTest
    public void shulkerWith10IdenticalStacksSortedAsUnit(GameTestHelper helper) {
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(SWORDS_AND_CONTAINERS_CATEGORY);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        BlockPos swordsPos = new BlockPos(3, 1, 1);
        BlockPos containersPos = new BlockPos(5, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);
        TestHelper.placeSingleChest(helper, swordsPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, swordsPos, Direction.NORTH, "swords");
        TestHelper.placeSingleChest(helper, containersPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, containersPos, Direction.NORTH, "containers");

        // Create shulker with exactly 10 diamond swords (meets threshold)
        ItemStack[] contents = new ItemStack[10];
        for (int i = 0; i < 10; i++) {
            contents[i] = new ItemStack(Items.DIAMOND_SWORD);
        }
        ItemStack shulker = TestHelper.createShulkerBox(contents);
        TestHelper.insertItems(helper, inputPos, shulker);

        // Execute sort
        TestHelper.executeSort(helper, inputPos, 10);

        // Verify: The shulker itself should be in the swords category (sorted as unit)
        List<ItemStack> swordsChestContents = TestHelper.getChestContents(helper, swordsPos);
        boolean hasShulkerInSwords = swordsChestContents.stream()
            .anyMatch(s -> s.is(Items.SHULKER_BOX));

        if (!hasShulkerInSwords) {
            helper.fail(Component.literal("Shulker with 10 swords should be sorted to swords category as a unit"));
            return;
        }

        // Verify: The swords category chest should contain a shulker with 10 swords inside
        ItemStack sortedShulker = swordsChestContents.stream()
            .filter(s -> s.is(Items.SHULKER_BOX))
            .findFirst()
            .orElse(ItemStack.EMPTY);
        List<ItemStack> shulkerContents = TestHelper.getShulkerContents(sortedShulker);
        long swordCount = shulkerContents.stream().filter(s -> s.is(Items.DIAMOND_SWORD)).count();
        if (swordCount != 10) {
            helper.fail(Component.literal("Shulker should still contain 10 swords but has " + swordCount));
            return;
        }

        helper.succeed();
    }

    /**
     * Test that a shulker with 9 stacks of the same item has its contents extracted.
     * The threshold is 10, so 9 stacks means contents should be extracted individually.
     */
    @GameTest
    public void shulkerWith9IdenticalStacksExtracted(GameTestHelper helper) {
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(SWORDS_AND_CONTAINERS_CATEGORY);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        BlockPos swordsPos = new BlockPos(3, 1, 1);
        BlockPos containersPos = new BlockPos(5, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);
        TestHelper.placeSingleChest(helper, swordsPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, swordsPos, Direction.NORTH, "swords");
        TestHelper.placeSingleChest(helper, containersPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, containersPos, Direction.NORTH, "containers");

        // Create shulker with 9 diamond swords (below threshold)
        ItemStack[] contents = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            contents[i] = new ItemStack(Items.DIAMOND_SWORD);
        }
        ItemStack shulker = TestHelper.createShulkerBox(contents);
        TestHelper.insertItems(helper, inputPos, shulker);

        // Execute sort
        TestHelper.executeSort(helper, inputPos, 10);

        // Verify: Individual swords should be extracted to swords category
        int swordCount = TestHelper.countItemsInChest(helper, swordsPos, Items.DIAMOND_SWORD);
        if (swordCount != 9) {
            helper.fail(Component.literal("Expected 9 swords extracted to swords category but found " + swordCount));
            return;
        }

        // Verify: Empty shulker should be in containers category
        int shulkerCount = TestHelper.countItemsInChest(helper, containersPos, Items.SHULKER_BOX);
        if (shulkerCount != 1) {
            helper.fail(Component.literal("Expected empty shulker in containers category but found " + shulkerCount));
            return;
        }

        helper.succeed();
    }

    /**
     * Test that a shulker with 10+ stacks but 2 different items has contents extracted.
     * Only uniform (same item) containers qualify for threshold behavior.
     */
    @GameTest
    public void shulkerWithMixedItemsExtracted(GameTestHelper helper) {
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(SWORDS_AND_CONTAINERS_CATEGORY);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        BlockPos swordsPos = new BlockPos(3, 1, 1);
        BlockPos containersPos = new BlockPos(5, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);
        TestHelper.placeSingleChest(helper, swordsPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, swordsPos, Direction.NORTH, "swords");
        TestHelper.placeSingleChest(helper, containersPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, containersPos, Direction.NORTH, "containers");

        // Create shulker with 10 stacks but mixed items (5 diamond + 5 iron)
        ItemStack[] contents = new ItemStack[10];
        for (int i = 0; i < 5; i++) {
            contents[i] = new ItemStack(Items.DIAMOND_SWORD);
        }
        for (int i = 5; i < 10; i++) {
            contents[i] = new ItemStack(Items.IRON_SWORD);
        }
        ItemStack shulker = TestHelper.createShulkerBox(contents);
        TestHelper.insertItems(helper, inputPos, shulker);

        // Execute sort
        TestHelper.executeSort(helper, inputPos, 10);

        // Verify: All swords should be extracted (not sorted as unit)
        int diamondSwords = TestHelper.countItemsInChest(helper, swordsPos, Items.DIAMOND_SWORD);
        int ironSwords = TestHelper.countItemsInChest(helper, swordsPos, Items.IRON_SWORD);
        if (diamondSwords != 5 || ironSwords != 5) {
            helper.fail(Component.literal("Expected 5 diamond and 5 iron swords but found " +
                diamondSwords + " diamond and " + ironSwords + " iron"));
            return;
        }

        // Verify: Empty shulker should be in containers category
        int shulkerCount = TestHelper.countItemsInChest(helper, containersPos, Items.SHULKER_BOX);
        if (shulkerCount != 1) {
            helper.fail(Component.literal("Expected empty shulker in containers category"));
            return;
        }

        helper.succeed();
    }

    // ========== Container Sorted to Own Category Tests ==========

    /**
     * Test that an empty shulker is sorted to the containers category.
     */
    @GameTest
    public void emptyShulkerSortedToContainersCategory(GameTestHelper helper) {
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(SWORDS_AND_CONTAINERS_CATEGORY);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        BlockPos containersPos = new BlockPos(3, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);
        TestHelper.placeSingleChest(helper, containersPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, containersPos, Direction.NORTH, "containers");

        // Create empty shulker
        ItemStack emptyShulker = new ItemStack(Items.SHULKER_BOX);
        TestHelper.insertItems(helper, inputPos, emptyShulker);

        // Execute sort
        TestHelper.executeSort(helper, inputPos);

        // Verify: Empty shulker should be in containers category
        int shulkerCount = TestHelper.countItemsInChest(helper, containersPos, Items.SHULKER_BOX);
        if (shulkerCount != 1) {
            helper.fail(Component.literal("Expected empty shulker in containers category but found " + shulkerCount));
            return;
        }

        // Verify: Input should be empty
        TestHelper.assertChestEmpty(helper, inputPos);

        helper.succeed();
    }

    /**
     * Test that an empty bundle is sorted to the containers category.
     */
    @GameTest
    public void emptyBundleSortedToContainersCategory(GameTestHelper helper) {
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(SWORDS_AND_CONTAINERS_CATEGORY);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        BlockPos containersPos = new BlockPos(3, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);
        TestHelper.placeSingleChest(helper, containersPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, containersPos, Direction.NORTH, "containers");

        // Create empty bundle
        ItemStack emptyBundle = new ItemStack(Items.BUNDLE);
        TestHelper.insertItems(helper, inputPos, emptyBundle);

        // Execute sort
        TestHelper.executeSort(helper, inputPos);

        // Verify: Empty bundle should be in containers category
        int bundleCount = TestHelper.countItemsInChest(helper, containersPos, Items.BUNDLE);
        if (bundleCount != 1) {
            helper.fail(Component.literal("Expected empty bundle in containers category but found " + bundleCount));
            return;
        }

        // Verify: Input should be empty
        TestHelper.assertChestEmpty(helper, inputPos);

        helper.succeed();
    }

    /**
     * Test that after extracting contents, the container goes to its category.
     * Shulker with sortable items → items sorted, then shulker sorted to containers.
     */
    @GameTest
    public void shulkerSortedToContainersAfterExtraction(GameTestHelper helper) {
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(SWORDS_AND_CONTAINERS_CATEGORY);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        BlockPos swordsPos = new BlockPos(3, 1, 1);
        BlockPos containersPos = new BlockPos(5, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);
        TestHelper.placeSingleChest(helper, swordsPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, swordsPos, Direction.NORTH, "swords");
        TestHelper.placeSingleChest(helper, containersPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, containersPos, Direction.NORTH, "containers");

        // Create shulker with 3 swords (below threshold, will be extracted)
        ItemStack shulker = TestHelper.createShulkerBox(
            new ItemStack(Items.DIAMOND_SWORD),
            new ItemStack(Items.DIAMOND_SWORD),
            new ItemStack(Items.DIAMOND_SWORD)
        );
        TestHelper.insertItems(helper, inputPos, shulker);

        // Execute sort
        TestHelper.executeSort(helper, inputPos, 10);

        // Verify: Swords should be in swords category
        int swordCount = TestHelper.countItemsInChest(helper, swordsPos, Items.DIAMOND_SWORD);
        if (swordCount != 3) {
            helper.fail(Component.literal("Expected 3 swords in swords category but found " + swordCount));
            return;
        }

        // Verify: Empty shulker should be in containers category
        int shulkerCount = TestHelper.countItemsInChest(helper, containersPos, Items.SHULKER_BOX);
        if (shulkerCount != 1) {
            helper.fail(Component.literal("Expected shulker in containers category but found " + shulkerCount));
            return;
        }

        // Verify: Input should be empty
        TestHelper.assertChestEmpty(helper, inputPos);

        helper.succeed();
    }

    /**
     * Test that after extracting contents, the bundle goes to its category.
     */
    @GameTest
    public void bundleSortedToContainersAfterExtraction(GameTestHelper helper) {
        CategoryLoader.clear();
        CategoryLoader.loadCategoriesFromYaml(SWORDS_AND_CONTAINERS_CATEGORY);
        CategoryLoader.flattenCategories();

        BlockPos inputPos = new BlockPos(1, 1, 1);
        BlockPos swordsPos = new BlockPos(3, 1, 1);
        BlockPos containersPos = new BlockPos(5, 1, 1);

        TestHelper.placeSingleChest(helper, inputPos, Direction.NORTH);
        TestHelper.placeInputSign(helper, inputPos, Direction.NORTH);
        TestHelper.placeSingleChest(helper, swordsPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, swordsPos, Direction.NORTH, "swords");
        TestHelper.placeSingleChest(helper, containersPos, Direction.NORTH);
        TestHelper.placeCategorySign(helper, containersPos, Direction.NORTH, "containers");

        // Create bundle with 2 swords
        ItemStack bundle = TestHelper.createBundle(
            new ItemStack(Items.DIAMOND_SWORD),
            new ItemStack(Items.IRON_SWORD)
        );
        TestHelper.insertItems(helper, inputPos, bundle);

        // Execute sort
        TestHelper.executeSort(helper, inputPos, 10);

        // Verify: Swords should be in swords category
        int swordCount = TestHelper.countItemsInChest(helper, swordsPos, Items.DIAMOND_SWORD) +
                         TestHelper.countItemsInChest(helper, swordsPos, Items.IRON_SWORD);
        if (swordCount != 2) {
            helper.fail(Component.literal("Expected 2 swords in swords category but found " + swordCount));
            return;
        }

        // Verify: Empty bundle should be in containers category
        int bundleCount = TestHelper.countItemsInChest(helper, containersPos, Items.BUNDLE);
        if (bundleCount != 1) {
            helper.fail(Component.literal("Expected bundle in containers category but found " + bundleCount));
            return;
        }

        // Verify: Input should be empty
        TestHelper.assertChestEmpty(helper, inputPos);

        helper.succeed();
    }
}
