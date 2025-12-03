package net.sortcraft;

import net.minecraft.world.item.ItemStack;

/**
 * Interface for filter rules that match item stacks.
 */
public interface FilterRule {
    boolean matches(ItemStack stack);
}

