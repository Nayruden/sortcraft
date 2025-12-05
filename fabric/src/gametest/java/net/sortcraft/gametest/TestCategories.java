package net.sortcraft.gametest;

/**
 * Shared category YAML definitions for game tests.
 * Uses complete YAML strings to ensure proper formatting.
 */
public final class TestCategories {
    private TestCategories() {}

    // ========== Single Category Definitions ==========

    /** Single category: cobblestone only */
    public static final String COBBLESTONE = """
        cobblestone:
          items:
            - minecraft:cobblestone
        """;

    /** Single category: all sword types */
    public static final String SWORDS = """
        swords:
          items:
            - minecraft:diamond_sword
            - minecraft:iron_sword
            - minecraft:stone_sword
            - minecraft:wooden_sword
            - minecraft:golden_sword
            - minecraft:netherite_sword
        """;

    /** Single category: diamond and iron swords only */
    public static final String SWORDS_SHORT = """
        swords:
          items:
            - minecraft:diamond_sword
            - minecraft:iron_sword
        """;

    /** Single category: eggs (stack size 16) */
    public static final String EGGS = """
        eggs:
          items:
            - minecraft:egg
        """;

    /** Single category: ender pearls (stack size 16) */
    public static final String PEARLS = """
        pearls:
          items:
            - minecraft:ender_pearl
        """;

    /** Single category: diamond and iron pickaxes and axes */
    public static final String TOOLS = """
        tools:
          items:
            - minecraft:diamond_pickaxe
            - minecraft:iron_pickaxe
            - minecraft:diamond_axe
            - minecraft:iron_axe
        """;

    /** Single category: gems (diamonds and emeralds) */
    public static final String GEMS = """
        gems:
          items:
            - minecraft:diamond
            - minecraft:emerald
        """;

    // ========== Multi-Category Definitions ==========

    /** Two categories: swords and containers */
    public static final String SWORDS_AND_CONTAINERS = """
        swords:
          items:
            - minecraft:diamond_sword
            - minecraft:iron_sword
        containers:
          items:
            - minecraft:shulker_box
            - minecraft:bundle
        """;

    /** Three categories: swords, pickaxes, cobblestone */
    public static final String STORAGE = """
        swords:
          items:
            - minecraft:diamond_sword
            - minecraft:iron_sword
        pickaxes:
          items:
            - minecraft:diamond_pickaxe
            - minecraft:iron_pickaxe
        cobblestone:
          items:
            - minecraft:cobblestone
        """;

    /** Four categories for audit tests: swords, cobblestone, gems, containers */
    public static final String AUDIT_MULTI = """
        swords:
          items:
            - minecraft:diamond_sword
            - minecraft:iron_sword
        cobblestone:
          items:
            - minecraft:cobblestone
        gems:
          items:
            - minecraft:diamond
            - minecraft:emerald
        containers:
          items:
            - minecraft:shulker_box
            - minecraft:bundle
        """;

    /** Four categories: swords, pickaxes, axes, cobblestone (for performance tests) */
    public static final String MULTI_TOOLS = """
        swords:
          items:
            - minecraft:diamond_sword
            - minecraft:iron_sword
            - minecraft:stone_sword
            - minecraft:wooden_sword
            - minecraft:golden_sword
            - minecraft:netherite_sword
        pickaxes:
          items:
            - minecraft:diamond_pickaxe
            - minecraft:iron_pickaxe
            - minecraft:stone_pickaxe
            - minecraft:wooden_pickaxe
            - minecraft:golden_pickaxe
            - minecraft:netherite_pickaxe
        axes:
          items:
            - minecraft:diamond_axe
            - minecraft:iron_axe
            - minecraft:stone_axe
            - minecraft:wooden_axe
            - minecraft:golden_axe
            - minecraft:netherite_axe
        cobblestone:
          items:
            - minecraft:cobblestone
        """;

    // ========== Priority Category Definitions ==========

    /** Two categories with different priorities for the same item */
    public static final String PRIORITY = """
        high_priority:
          priority: 1
          items:
            - minecraft:diamond_sword
        low_priority:
          priority: 20
          items:
            - minecraft:diamond_sword
        """;

    /** Category with default priority (10) and explicit priority (10) */
    public static final String DEFAULT_PRIORITY = """
        default_cat:
          items:
            - minecraft:diamond_sword
        explicit_ten:
          priority: 10
          items:
            - minecraft:iron_sword
        """;

    // ========== Tag-Based Category Definitions ==========

    /** Category using vanilla logs tag */
    public static final String LOGS = """
        logs:
          items:
            - "#minecraft:logs"
        """;

    /** Category using vanilla planks tag */
    public static final String PLANKS = """
        planks:
          items:
            - "#minecraft:planks"
        """;

    /** Category with both tags and explicit items */
    public static final String LOGS_AND_STICKS = """
        wood_items:
          items:
            - "#minecraft:logs"
            - minecraft:stick
        """;

    /** Category with non-existent tag (for error handling test) */
    public static final String INVALID_TAG = """
        invalid:
          items:
            - "#minecraft:nonexistent_tag_that_does_not_exist"
        """;

    /** Category using regex pattern for swords (for migration test) */
    public static final String SWORDS_REGEX = """
        swords:
          items:
            - /.*:.*_sword/
        """;

    /** Hierarchical categories with includes for priority testing */
    public static final String HIERARCHY = """
        swords:
          priority: 10
          items:
            - /.*:.*_sword/

        weapons:
          priority: 15
          includes:
            - swords

        gear:
          priority: 20
          includes:
            - weapons
        """;
}

