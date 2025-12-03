package net.sortcraft.compat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;

/**
 * Entity helper for Minecraft 1.21.4+
 * In 1.21.4+, EntityType.create() requires an EntitySpawnReason parameter.
 */
public final class EntityHelper {
    private EntityHelper() {}

    /**
     * Creates an entity of the given type in the world.
     * Uses EntitySpawnReason.COMMAND for programmatic entity creation.
     */
    public static <T extends Entity> T create(EntityType<T> entityType, ServerLevel world) {
        return entityType.create(world, EntitySpawnReason.COMMAND);
    }
}

