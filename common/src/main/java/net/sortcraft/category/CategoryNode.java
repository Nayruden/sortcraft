package net.sortcraft.category;

import net.minecraft.resources.ResourceLocation;
import net.sortcraft.FilterRule;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a sorting category with its configuration.
 * Categories define which items belong to them and can include other categories.
 */
public class CategoryNode implements Comparable<CategoryNode> {
    public final String name;
    public final Set<String> includes = new HashSet<>();
    public final Set<ResourceLocation> itemIds = new HashSet<>();
    public Set<ResourceLocation> flattenedItemIds = null;
    public final List<FilterRule> filters = new ArrayList<>();
    public int priority = 10;

    public CategoryNode(String name) {
        this.name = name;
    }

    public static String categoriesToStr(Collection<CategoryNode> categories) {
        return categories.stream().map(CategoryNode::toString).collect(Collectors.joining(", "));
    }

    @Override
    public int compareTo(CategoryNode other) {
        return Integer.compare(priority, other.priority);
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CategoryNode)) return false;
        CategoryNode categoryNode = (CategoryNode) o;
        return Objects.equals(name, categoryNode.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}

