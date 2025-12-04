package net.sortcraft;

import net.minecraft.core.RegistryAccess;

/**
 * Factory for creating FilterRule instances from YAML configuration.
 */
public final class FilterRuleFactory {
    private FilterRuleFactory() {}

    /**
     * Creates a FilterRule from YAML configuration.
     *
     * @param registries the registry access (can be null for filters that don't need it)
     * @param key the filter key (e.g., "custom_name", "stackable", "enchantment")
     * @param value the filter value
     * @return the created FilterRule
     * @throws IllegalArgumentException if the key is null/empty, or if registries is null but required by the filter
     */
    public static FilterRule fromYaml(RegistryAccess registries, String key, String value) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Filter key cannot be null or empty");
        }

        // Delegate to Filters class for actual implementation
        return Filters.createFilterRule(registries, key, value);
    }
}

