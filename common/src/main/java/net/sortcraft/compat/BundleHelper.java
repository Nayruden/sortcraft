package net.sortcraft.compat;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.BundleContents;

import java.util.ArrayList;
import java.util.List;

/**
 * Bundle compat helper for Minecraft 26.1+.
 * BundleContents uses ItemStackTemplate instead of ItemStack in 26.1+.
 */
public final class BundleHelper {
    private BundleHelper() {}

    /**
     * Extract items from a BundleContents as a list of ItemStack.
     * In 26.1+, BundleContents.items() yields ItemStackTemplate, so we convert via create().
     */
    public static List<ItemStack> getItems(BundleContents bundle) {
        List<ItemStack> items = new ArrayList<>();
        bundle.items().forEach(template -> items.add(template.create()));
        return items;
    }

    /**
     * Create a BundleContents from a list of ItemStack.
     * In 26.1+, BundleContents constructor expects List&lt;ItemStackTemplate&gt;.
     */
    public static BundleContents create(List<ItemStack> items) {
        List<ItemStackTemplate> templates = items.stream()
                .map(ItemStackTemplate::fromNonEmptyStack)
                .toList();
        return new BundleContents(templates);
    }
}

