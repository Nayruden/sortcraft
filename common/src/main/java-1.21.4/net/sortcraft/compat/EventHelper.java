package net.sortcraft.compat;

import net.minecraft.world.InteractionResult;

/**
 * Event helper for Minecraft 1.21.4+
 * In 1.21.4+, InteractionEvent.RightClickBlock returns InteractionResult
 */
public final class EventHelper {
    private EventHelper() {}

    /**
     * Get the PASS result for interaction events.
     */
    public static InteractionResult pass() {
        return InteractionResult.PASS;
    }

    /**
     * Get the SUCCESS result for interaction events.
     */
    public static InteractionResult success() {
        return InteractionResult.SUCCESS;
    }

    /**
     * Get the FAIL result for interaction events.
     */
    public static InteractionResult fail() {
        return InteractionResult.FAIL;
    }
}

