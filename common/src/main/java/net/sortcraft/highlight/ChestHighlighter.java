package net.sortcraft.highlight;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.sortcraft.compat.EntityHelper;
import net.sortcraft.container.ContainerHelper;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages glowing chest highlights using invisible MagmaCube entities.
 * Uses MagmaCubes instead of Shulkers to avoid blocking chest interaction.
 * Highlights are visible through walls.
 */
public final class ChestHighlighter {
    private ChestHighlighter() {}

    // Track active chest highlights: entity UUID -> ticks remaining
    private static final Map<UUID, Integer> activeHighlights = new ConcurrentHashMap<>();

    /**
     * Highlights a chest (or double chest) with a glowing outline visible through walls.
     * For double chests, both halves are highlighted.
     */
    public static void highlightChest(ServerLevel world, BlockPos pos, int durationTicks, ChatFormatting color) {
        List<BlockPos> chestBlocks = ContainerHelper.getChestBlocks(pos, world);
        for (BlockPos blockPos : chestBlocks) {
            spawnHighlightMarker(world, blockPos, durationTicks, color);
        }
    }

    private static void spawnHighlightMarker(ServerLevel world, BlockPos pos, int durationTicks, ChatFormatting color) {
        MagmaCube marker = EntityHelper.create(EntityType.MAGMA_CUBE, world);
        if (marker == null) return;

        // Size 1 is the smallest MagmaCube size, giving a small hitbox that doesn't block chest interaction
        marker.setSize(1, false);
        marker.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        marker.setInvisible(true);
        marker.setInvulnerable(true);
        marker.setNoAi(true);
        marker.setSilent(true);
        marker.setNoGravity(true);
        // Disable physics/collision to further reduce interaction blocking
        marker.noPhysics = true;
        // Glowing effect - visible through walls. Add extra time buffer for cleanup lag.
        marker.addEffect(new MobEffectInstance(MobEffects.GLOWING, durationTicks + 20, 0, false, false));
        // Invisibility effect - makes the magma cube completely transparent, showing only the outline
        marker.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, durationTicks + 20, 0, false, false));

        // Set team color for the glow
        if (color != null) {
            Scoreboard scoreboard = world.getScoreboard();
            String teamName = "sortcraft_" + color.getName();
            PlayerTeam team = scoreboard.getPlayerTeam(teamName);
            if (team == null) {
                team = scoreboard.addPlayerTeam(teamName);
                team.setColor(color);
            }
            scoreboard.addPlayerToTeam(marker.getStringUUID(), team);
        }

        world.addFreshEntity(marker);
        activeHighlights.put(marker.getUUID(), durationTicks);
    }

    /**
     * Called every server tick to manage highlight entity lifetime.
     */
    public static void tick(MinecraftServer server) {
        if (activeHighlights.isEmpty()) return;

        Iterator<Map.Entry<UUID, Integer>> it = activeHighlights.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> entry = it.next();
            int remaining = entry.getValue() - 1;

            if (remaining <= 0) {
                // Find and remove the entity
                for (ServerLevel level : server.getAllLevels()) {
                    Entity entity = level.getEntity(entry.getKey());
                    if (entity != null) {
                        entity.discard();
                        break;
                    }
                }
                it.remove();
            } else {
                entry.setValue(remaining);
            }
        }
    }

    /**
     * Clears all active highlights. Called on server stop.
     */
    public static void clearAll() {
        activeHighlights.clear();
    }
}

