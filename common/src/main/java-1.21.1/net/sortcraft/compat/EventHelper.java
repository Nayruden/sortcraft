package net.sortcraft.compat;

import dev.architectury.event.EventResult;

/**
 * Event helper for Minecraft 1.21.1
 * In 1.21.1, InteractionEvent.RightClickBlock returns EventResult
 */
public final class EventHelper {
    private EventHelper() {}

    /**
     * Get the PASS result for interaction events.
     */
    public static EventResult pass() {
        return EventResult.pass();
    }

    /**
     * Get the SUCCESS result for interaction events.
     */
    public static EventResult success() {
        return EventResult.interruptTrue();
    }

    /**
     * Get the FAIL result for interaction events.
     */
    public static EventResult fail() {
        return EventResult.interruptFalse();
    }
}

