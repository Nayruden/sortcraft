package net.sortcraft;

import net.minecraft.server.MinecraftServer;

/**
 * Factory for creating FilterRule instances from YAML configuration.
 */
public final class FilterRuleFactory {
    private FilterRuleFactory() {}

    public static FilterRule fromYaml(MinecraftServer server, String key, String value) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Filter key cannot be null or empty");
        }
        if (server == null) {
            throw new IllegalArgumentException("Server cannot be null");
        }

        // Delegate to Filters class for actual implementation
        return Filters.createFilterRule(server, key, value);
    }
}

