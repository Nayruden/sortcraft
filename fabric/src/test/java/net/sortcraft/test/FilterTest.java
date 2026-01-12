package net.sortcraft.test;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.sortcraft.category.CategoryLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bootstrap tests for filter functionality.
 * Tests enchantment, custom_name, stackable, and negation filters.
 */
public class FilterTest extends SortCraftBootstrapTestBase {

    // ========== Custom Name Filter ==========

    @Test
    void customNameFilterMatchesExactName() {
        String yaml = """
            named_swords:
              items:
              - minecraft:diamond_sword
              filters:
              - custom_name: "Excalibur"
            """;
        loadCategories(yaml);

        // Create sword with custom name
        ItemStack namedSword = new ItemStack(Items.DIAMOND_SWORD);
        namedSword.set(DataComponents.CUSTOM_NAME, Component.literal("Excalibur"));

        // Create sword without custom name
        ItemStack plainSword = new ItemStack(Items.DIAMOND_SWORD);

        assertStackMatchesCategory(namedSword, "named_swords");
        assertStackDoesNotMatchCategory(plainSword, "named_swords");
    }

    @Test
    void customNameFilterWildcardMatchesAnyName() {
        String yaml = """
            any_named:
              items:
              - minecraft:diamond_sword
              filters:
              - custom_name: "*"
            """;
        loadCategories(yaml);

        ItemStack namedSword = new ItemStack(Items.DIAMOND_SWORD);
        namedSword.set(DataComponents.CUSTOM_NAME, Component.literal("Any Name"));

        ItemStack plainSword = new ItemStack(Items.DIAMOND_SWORD);

        assertStackMatchesCategory(namedSword, "any_named");
        assertStackDoesNotMatchCategory(plainSword, "any_named");
    }

    @Test
    void customNameFilterCaseInsensitive() {
        String yaml = """
            named:
              items:
              - minecraft:diamond_sword
              filters:
              - custom_name: "excalibur"
            """;
        loadCategories(yaml);

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.set(DataComponents.CUSTOM_NAME, Component.literal("EXCALIBUR"));

        assertStackMatchesCategory(sword, "named");
    }

    // ========== Stackable Filter ==========

    @Test
    void stackableFilterMatchesStackableItems() {
        String yaml = """
            stackable:
              items:
              - minecraft:cobblestone
              - minecraft:diamond_sword
              filters:
              - stackable: "true"
            """;
        loadCategories(yaml);

        // Cobblestone is stackable (64)
        assertStackMatchesCategory(new ItemStack(Items.COBBLESTONE), "stackable");

        // Diamond sword is not stackable (1)
        assertStackDoesNotMatchCategory(new ItemStack(Items.DIAMOND_SWORD), "stackable");
    }

    // ========== Negation Filter ==========

    @Test
    void negatedCustomNameFilter() {
        String yaml = """
            unnamed:
              items:
              - minecraft:diamond_sword
              filters:
              - "!custom_name": "*"
            """;
        loadCategories(yaml);

        ItemStack namedSword = new ItemStack(Items.DIAMOND_SWORD);
        namedSword.set(DataComponents.CUSTOM_NAME, Component.literal("Named"));

        ItemStack plainSword = new ItemStack(Items.DIAMOND_SWORD);

        // Negated: matches items WITHOUT custom name
        assertStackMatchesCategory(plainSword, "unnamed");
        assertStackDoesNotMatchCategory(namedSword, "unnamed");
    }

    @Test
    void negatedStackableFilter() {
        String yaml = """
            unstackable:
              items:
              - minecraft:cobblestone
              - minecraft:diamond_sword
              filters:
              - "!stackable": "true"
            """;
        loadCategories(yaml);

        // Negated: matches items that are NOT stackable
        assertStackDoesNotMatchCategory(new ItemStack(Items.COBBLESTONE), "unstackable");
        assertStackMatchesCategory(new ItemStack(Items.DIAMOND_SWORD), "unstackable");
    }

    // ========== Filter Combinations ==========

    @Test
    void multipleFiltersAllMustMatch() {
        String yaml = """
            named_stackable:
              items:
              - minecraft:cobblestone
              filters:
              - custom_name: "*"
              - stackable: "true"
            """;
        loadCategories(yaml);

        ItemStack namedCobble = new ItemStack(Items.COBBLESTONE);
        namedCobble.set(DataComponents.CUSTOM_NAME, Component.literal("Special Cobble"));

        ItemStack plainCobble = new ItemStack(Items.COBBLESTONE);

        // Both filters must match
        assertStackMatchesCategory(namedCobble, "named_stackable");
        assertStackDoesNotMatchCategory(plainCobble, "named_stackable");
    }

    // ========== Edge Cases ==========

    @Test
    void filterOnEmptyStack() {
        String yaml = """
            named:
              items:
              - minecraft:diamond_sword
              filters:
              - custom_name: "*"
            """;
        loadCategories(yaml);

        // Empty stack should not match
        assertStackDoesNotMatchCategory(ItemStack.EMPTY, "named");
    }

    @Test
    void categoryWithoutFilterMatchesAll() {
        String yaml = """
            all_swords:
              items:
              - minecraft:diamond_sword
            """;
        loadCategories(yaml);

        ItemStack namedSword = new ItemStack(Items.DIAMOND_SWORD);
        namedSword.set(DataComponents.CUSTOM_NAME, Component.literal("Named"));

        ItemStack plainSword = new ItemStack(Items.DIAMOND_SWORD);

        // Without filter, both should match
        assertStackMatchesCategory(namedSword, "all_swords");
        assertStackMatchesCategory(plainSword, "all_swords");
    }

    @Test
    void filterWithPriorityOrdering() {
        String yaml = """
            named_swords:
              priority: 100
              items:
              - minecraft:diamond_sword
              filters:
              - custom_name: "*"
            all_swords:
              priority: 10
              items:
              - minecraft:diamond_sword
            """;
        loadCategories(yaml);

        ItemStack namedSword = new ItemStack(Items.DIAMOND_SWORD);
        namedSword.set(DataComponents.CUSTOM_NAME, Component.literal("Named"));

        ItemStack plainSword = new ItemStack(Items.DIAMOND_SWORD);

        // Named sword should match both, with lower priority (10) first
        assertStackMatchesCategoriesInOrder(namedSword, "all_swords", "named_swords");

        // Plain sword should only match all_swords
        assertStackMatchesCategory(plainSword, "all_swords");
        assertStackDoesNotMatchCategory(plainSword, "named_swords");
    }

    // ========== Filter + Include Interaction ==========

    @Test
    void filterAppliedToIncludedCategoryItems() {
        // Filters should apply to items inherited from includes
        String yaml = """
            swords:
              items:
              - minecraft:diamond_sword
              - minecraft:iron_sword
            named_weapons:
              includes:
              - swords
              filters:
              - custom_name: "*"
            """;
        loadCategories(yaml);

        ItemStack namedDiamond = namedStack(Items.DIAMOND_SWORD, "Named");
        ItemStack plainIron = new ItemStack(Items.IRON_SWORD);

        // Both items should match 'swords'
        assertStackMatchesCategory(namedDiamond, "swords");
        assertStackMatchesCategory(plainIron, "swords");

        // Only named item should match 'named_weapons' due to filter
        assertStackMatchesCategory(namedDiamond, "named_weapons");
        assertStackDoesNotMatchCategory(plainIron, "named_weapons");
    }

    @Test
    void filterPriorityWithIncludedCategories() {
        // Test priority ordering when filter categories include other categories
        String yaml = """
            all_swords:
              priority: 100
              items:
              - minecraft:diamond_sword
            named_swords:
              priority: 50
              includes:
              - all_swords
              filters:
              - custom_name: "*"
            """;
        loadCategories(yaml);

        ItemStack namedSword = namedStack(Items.DIAMOND_SWORD, "Named");

        // Should match named_swords (priority 50) before all_swords (priority 100)
        assertStackMatchesCategoriesInOrder(namedSword, "named_swords", "all_swords");
    }

    @Test
    void deepIncludeChainWithFilter() {
        // Test that filters work with deeply nested includes
        String yaml = """
            base:
              items:
              - minecraft:diamond_sword
            middle:
              includes:
              - base
            top:
              includes:
              - middle
              filters:
              - custom_name: "*"
            """;
        loadCategories(yaml);

        ItemStack namedSword = namedStack(Items.DIAMOND_SWORD, "Named");
        ItemStack plainSword = new ItemStack(Items.DIAMOND_SWORD);

        // Both should match base and middle (no filters)
        assertStackMatchesCategory(namedSword, "base");
        assertStackMatchesCategory(namedSword, "middle");
        assertStackMatchesCategory(plainSword, "base");
        assertStackMatchesCategory(plainSword, "middle");

        // Only named should match top (has filter)
        assertStackMatchesCategory(namedSword, "top");
        assertStackDoesNotMatchCategory(plainSword, "top");
    }

    // ========== Error Cases ==========

    @Test
    void unknownFilterKeySkipsCategory() {
        // Unknown filter keys should cause the category to be skipped (not loaded)
        String yaml = """
            bad_filter:
              items:
              - minecraft:diamond_sword
              filters:
              - unknown_filter: "value"
            """;

        int loaded = loadCategories(yaml);

        // Category should NOT be loaded due to invalid filter
        assertEquals(0, loaded, "Category with unknown filter should not be loaded");
        assertNull(CategoryLoader.getCategories().get("bad_filter"),
                "Category with unknown filter should not exist");
    }

    @Test
    void emptyCustomNameValueSkipsCategory() {
        // Empty filter value should cause the category to be skipped
        String yaml = """
            bad_filter:
              items:
              - minecraft:diamond_sword
              filters:
              - custom_name: ""
            """;

        int loaded = loadCategories(yaml);

        // Category should NOT be loaded due to empty filter value
        assertEquals(0, loaded, "Category with empty filter value should not be loaded");
    }

    @Test
    void validCategoryLoadedDespiteInvalidSibling() {
        // One invalid category should not prevent loading other valid categories
        String yaml = """
            valid_category:
              items:
              - minecraft:diamond_sword

            invalid_category:
              items:
              - minecraft:iron_sword
              filters:
              - unknown_filter: "bad"
            """;

        int loaded = loadCategories(yaml);

        // Only the valid category should be loaded
        assertEquals(1, loaded, "Only valid categories should be loaded");
        assertNotNull(CategoryLoader.getCategories().get("valid_category"),
                "Valid category should exist");
        assertNull(CategoryLoader.getCategories().get("invalid_category"),
                "Invalid category should not exist");
    }

    // ========== Helper method tests ==========

    @Test
    void namedStackHelperCreatesCorrectStack() {
        // Test that the namedStack helper creates stacks correctly
        ItemStack stack = namedStack(Items.DIAMOND_SWORD, "TestName");

        assertEquals(Items.DIAMOND_SWORD, stack.getItem());
        assertEquals(1, stack.getCount());
        Component customName = stack.get(DataComponents.CUSTOM_NAME);
        assertNotNull(customName);
        assertEquals("TestName", customName.getString());
    }

    @Test
    void namedStackWithCountCreatesCorrectStack() {
        // Test the count variant
        ItemStack stack = namedStack(Items.COBBLESTONE, 32, "SpecialCobble");

        assertEquals(Items.COBBLESTONE, stack.getItem());
        assertEquals(32, stack.getCount());
        Component customName = stack.get(DataComponents.CUSTOM_NAME);
        assertNotNull(customName);
        assertEquals("SpecialCobble", customName.getString());
    }

    @Test
    void filterTestWithNamedStackHelper() {
        // Verify namedStack works with filter matching
        String yaml = """
            named_swords:
              items:
              - minecraft:diamond_sword
              filters:
              - custom_name: "*"
            """;
        loadCategories(yaml);

        ItemStack named = namedStack(Items.DIAMOND_SWORD, "Excalibur");
        ItemStack plain = new ItemStack(Items.DIAMOND_SWORD);

        assertStackMatchesCategory(named, "named_swords");
        assertStackDoesNotMatchCategory(plain, "named_swords");
    }

    // ========== Durability Filter ==========

    /**
     * Helper to create a damaged item with specific durability percentage.
     * @param item The damageable item
     * @param durabilityPercent The desired durability (0-100)
     * @return ItemStack with the specified durability
     */
    private ItemStack damagedStack(net.minecraft.world.item.Item item, int durabilityPercent) {
        ItemStack stack = new ItemStack(item);
        int maxDamage = stack.getMaxDamage();
        if (maxDamage == 0) {
            throw new IllegalArgumentException("Item " + item + " is not damageable");
        }
        // durability% = (maxDamage - damage) / maxDamage * 100
        // damage = maxDamage - (durability% * maxDamage / 100)
        int damage = maxDamage - (durabilityPercent * maxDamage / 100);
        stack.setDamageValue(damage);
        return stack;
    }

    @Test
    void durabilityFilterMatchesDamagedItems() {
        String yaml = """
            damaged_tools:
              items:
              - minecraft:diamond_pickaxe
              filters:
              - durability: "<50%"
            """;
        loadCategories(yaml);

        // 25% durability should match <50%
        ItemStack damaged = damagedStack(Items.DIAMOND_PICKAXE, 25);
        assertStackMatchesCategory(damaged, "damaged_tools");

        // 75% durability should NOT match <50%
        ItemStack healthy = damagedStack(Items.DIAMOND_PICKAXE, 75);
        assertStackDoesNotMatchCategory(healthy, "damaged_tools");

        // 100% (pristine) should NOT match <50%
        ItemStack pristine = new ItemStack(Items.DIAMOND_PICKAXE);
        assertStackDoesNotMatchCategory(pristine, "damaged_tools");
    }

    @Test
    void durabilityFilterMatchesPristineItems() {
        String yaml = """
            pristine_armor:
              items:
              - minecraft:diamond_chestplate
              filters:
              - durability: "=100%"
            """;
        loadCategories(yaml);

        // Pristine (no damage) should match =100%
        ItemStack pristine = new ItemStack(Items.DIAMOND_CHESTPLATE);
        assertStackMatchesCategory(pristine, "pristine_armor");

        // Any damage should NOT match =100%
        ItemStack damaged = damagedStack(Items.DIAMOND_CHESTPLATE, 99);
        assertStackDoesNotMatchCategory(damaged, "pristine_armor");
    }

    @Test
    void durabilityFilterWithGreaterThan() {
        String yaml = """
            healthy_tools:
              items:
              - minecraft:iron_sword
              filters:
              - durability: ">75%"
            """;
        loadCategories(yaml);

        // 90% should match >75%
        ItemStack healthy = damagedStack(Items.IRON_SWORD, 90);
        assertStackMatchesCategory(healthy, "healthy_tools");

        // 75% should NOT match >75% (not greater than)
        ItemStack borderline = damagedStack(Items.IRON_SWORD, 75);
        assertStackDoesNotMatchCategory(borderline, "healthy_tools");

        // 50% should NOT match >75%
        ItemStack damaged = damagedStack(Items.IRON_SWORD, 50);
        assertStackDoesNotMatchCategory(damaged, "healthy_tools");
    }

    @Test
    void durabilityFilterWithGreaterThanOrEqual() {
        String yaml = """
            usable_tools:
              items:
              - minecraft:iron_axe
              filters:
              - durability: ">=50%"
            """;
        loadCategories(yaml);

        // 50% should match >=50%
        ItemStack borderline = damagedStack(Items.IRON_AXE, 50);
        assertStackMatchesCategory(borderline, "usable_tools");

        // 75% should match >=50%
        ItemStack healthy = damagedStack(Items.IRON_AXE, 75);
        assertStackMatchesCategory(healthy, "usable_tools");

        // 25% should NOT match >=50%
        ItemStack damaged = damagedStack(Items.IRON_AXE, 25);
        assertStackDoesNotMatchCategory(damaged, "usable_tools");
    }

    @Test
    void durabilityFilterWithLessThanOrEqual() {
        String yaml = """
            worn_gear:
              items:
              - minecraft:diamond_helmet
              filters:
              - durability: "<=25%"
            """;
        loadCategories(yaml);

        // 25% should match <=25%
        ItemStack borderline = damagedStack(Items.DIAMOND_HELMET, 25);
        assertStackMatchesCategory(borderline, "worn_gear");

        // 10% should match <=25%
        ItemStack veryDamaged = damagedStack(Items.DIAMOND_HELMET, 10);
        assertStackMatchesCategory(veryDamaged, "worn_gear");

        // 50% should NOT match <=25%
        ItemStack healthy = damagedStack(Items.DIAMOND_HELMET, 50);
        assertStackDoesNotMatchCategory(healthy, "worn_gear");
    }

    @Test
    void durabilityFilterWildcardMatchesDamageableItems() {
        String yaml = """
            damageable:
              items:
              - minecraft:diamond_sword
              - minecraft:stick
              filters:
              - durability: "*"
            """;
        loadCategories(yaml);

        // Diamond sword is damageable - should match
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        assertStackMatchesCategory(sword, "damageable");

        // Stick is NOT damageable - should NOT match
        ItemStack stick = new ItemStack(Items.STICK);
        assertStackDoesNotMatchCategory(stick, "damageable");
    }

    @Test
    void durabilityFilterNonDamageableItemsTreatedAs100Percent() {
        // Non-damageable items should be treated as 100% durability for comparison operators
        String yaml = """
            pristine:
              items:
              - minecraft:cobblestone
              filters:
              - durability: "=100%"
            damaged:
              items:
              - minecraft:cobblestone
              filters:
              - durability: "<100%"
            """;
        loadCategories(yaml);

        ItemStack cobble = new ItemStack(Items.COBBLESTONE);

        // Cobblestone (non-damageable) should match =100%
        assertStackMatchesCategory(cobble, "pristine");

        // Cobblestone should NOT match <100%
        assertStackDoesNotMatchCategory(cobble, "damaged");
    }

    @Test
    void durabilityFilterNegation() {
        String yaml = """
            not_damaged:
              items:
              - minecraft:diamond_sword
              filters:
              - "!durability": "<50%"
            """;
        loadCategories(yaml);

        // 75% durability should match (NOT <50%)
        ItemStack healthy = damagedStack(Items.DIAMOND_SWORD, 75);
        assertStackMatchesCategory(healthy, "not_damaged");

        // 25% durability should NOT match (it IS <50%)
        ItemStack damaged = damagedStack(Items.DIAMOND_SWORD, 25);
        assertStackDoesNotMatchCategory(damaged, "not_damaged");
    }

    @Test
    void durabilityFilterCombinedWithOtherFilters() {
        String yaml = """
            named_damaged:
              items:
              - minecraft:diamond_sword
              filters:
              - durability: "<50%"
              - custom_name: "*"
            """;
        loadCategories(yaml);

        // Named AND damaged should match
        ItemStack namedDamaged = damagedStack(Items.DIAMOND_SWORD, 25);
        namedDamaged.set(DataComponents.CUSTOM_NAME, Component.literal("Old Faithful"));
        assertStackMatchesCategory(namedDamaged, "named_damaged");

        // Damaged but NOT named should NOT match
        ItemStack unnamedDamaged = damagedStack(Items.DIAMOND_SWORD, 25);
        assertStackDoesNotMatchCategory(unnamedDamaged, "named_damaged");

        // Named but NOT damaged should NOT match
        ItemStack namedHealthy = new ItemStack(Items.DIAMOND_SWORD);
        namedHealthy.set(DataComponents.CUSTOM_NAME, Component.literal("Excalibur"));
        assertStackDoesNotMatchCategory(namedHealthy, "named_damaged");
    }

    @Test
    void durabilityFilterMultipleRanges() {
        // Test using multiple durability filters to create a range
        String yaml = """
            mid_durability:
              items:
              - minecraft:diamond_pickaxe
              filters:
              - durability: ">=25%"
              - durability: "<=75%"
            """;
        loadCategories(yaml);

        // 50% should match (between 25% and 75%)
        ItemStack mid = damagedStack(Items.DIAMOND_PICKAXE, 50);
        assertStackMatchesCategory(mid, "mid_durability");

        // 10% should NOT match (below 25%)
        ItemStack low = damagedStack(Items.DIAMOND_PICKAXE, 10);
        assertStackDoesNotMatchCategory(low, "mid_durability");

        // 90% should NOT match (above 75%)
        ItemStack high = damagedStack(Items.DIAMOND_PICKAXE, 90);
        assertStackDoesNotMatchCategory(high, "mid_durability");
    }

    @Test
    void durabilityFilterInvalidExpressionNotLoaded() {
        String yaml = """
            bad_durability:
              items:
              - minecraft:diamond_sword
              filters:
              - durability: "invalid"
            """;

        int loaded = loadCategories(yaml);

        // Category should NOT be loaded due to invalid filter expression
        assertEquals(0, loaded, "Category with invalid durability filter should not be loaded");
        assertNull(CategoryLoader.getCategories().get("bad_durability"),
                "Category with invalid durability filter should not exist");
    }

    @Test
    void durabilityFilterEmptyValueNotLoaded() {
        String yaml = """
            empty_durability:
              items:
              - minecraft:diamond_sword
              filters:
              - durability: ""
            """;

        int loaded = loadCategories(yaml);

        // Category should NOT be loaded due to empty filter value
        assertEquals(0, loaded, "Category with empty durability filter should not be loaded");
    }
}

