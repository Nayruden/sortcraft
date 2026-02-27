package net.sortcraft.test;

import net.sortcraft.category.CategoryLoader;
import net.sortcraft.category.CategorySet;
import net.sortcraft.container.SortContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bootstrap tests for {@link SortContext#getCategorySet()} fallback logic.
 * Verifies that the context defaults to the global CategorySet and can be overridden.
 */
public class SortContextCategorySetTest extends SortCraftBootstrapTestBase {

    @Test
    void defaultsToGlobalCategorySet() {
        loadCategories("""
            swords:
              items:
                - minecraft:diamond_sword
            """);

        // SortContext with null world/pos — getCategorySet() doesn't use them
        SortContext context = new SortContext(null, null, 10);

        CategorySet contextSet = context.getCategorySet();
        CategorySet globalSet = CategoryLoader.getGlobalCategorySet();

        assertSame(globalSet, contextSet, "Default getCategorySet() should return global set");
    }

    @Test
    void explicitCategorySetOverridesGlobal() {
        loadCategories("""
            swords:
              items:
                - minecraft:diamond_sword
            """);

        CategorySet customSet = CategoryLoader.loadIsolatedFromYaml("""
            pickaxes:
              items:
                - minecraft:diamond_pickaxe
            """);

        SortContext context = new SortContext(null, null, 10);
        context.setCategorySet(customSet);

        assertSame(customSet, context.getCategorySet(),
                "getCategorySet() should return explicitly set CategorySet");
        assertNotSame(CategoryLoader.getGlobalCategorySet(), context.getCategorySet(),
                "Should not return global set when explicit set provided");
    }

    @Test
    void explicitNullFallsBackToGlobal() {
        loadCategories("""
            swords:
              items:
                - minecraft:diamond_sword
            """);

        CategorySet customSet = CategoryLoader.loadIsolatedFromYaml("""
            pickaxes:
              items:
                - minecraft:diamond_pickaxe
            """);

        SortContext context = new SortContext(null, null, 10);
        context.setCategorySet(customSet);
        assertSame(customSet, context.getCategorySet());

        // Set back to null — should fall back to global
        context.setCategorySet(null);
        assertSame(CategoryLoader.getGlobalCategorySet(), context.getCategorySet(),
                "Setting null should fall back to global CategorySet");
    }
}

