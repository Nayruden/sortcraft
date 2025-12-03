package net.sortcraft.sorting;

import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * Holds the results of a sorting operation.
 */
public class SortingResults {
    public int sorted = 0;
    public final Set<String> unknownItems = new HashSet<>();
    public final Set<String> overflowCategories = new HashSet<>();
    public final List<ItemStack> leftovers = new ArrayList<>();
    public final Map<String, Integer> categoryCounts = new HashMap<>();
}

