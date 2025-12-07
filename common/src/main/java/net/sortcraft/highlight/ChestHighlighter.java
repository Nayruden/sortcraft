package net.sortcraft.highlight;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.sortcraft.compat.EntityHelper;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages glowing chest highlights using invisible Shulker entities.
 * Shulkers provide a visible glowing outline through walls.
 * Uses 0.85 scale to allow chest interaction around the edges.
 * For double chests, spawns a Shulker on each block.
 */
public final class ChestHighlighter {
    private ChestHighlighter() {}

    private static final double SHULKER_SCALE = 0.85;

    /** Tracks an active highlight entity. */
    private record HighlightEntry(ResourceKey<Level> dimension, int ticksRemaining) {}

    // Track active chest highlights: entity UUID -> highlight info
    private static final Map<UUID, HighlightEntry> activeHighlights = new ConcurrentHashMap<>();

    /**
     * Highlights a chest (or double chest) with a glowing outline visible through walls.
     * For double chests, spawns a Shulker on each block position.
     */
    public static void highlightChest(ServerLevel world, BlockPos pos, int durationTicks, ChatFormatting color) {
        BlockState state = world.getBlockState(pos);

        // Check if this is a double chest and highlight both blocks
        if (state.getBlock() instanceof ChestBlock) {
            ChestType chestType = state.getValue(BlockStateProperties.CHEST_TYPE);
            Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);

            if (chestType != ChestType.SINGLE) {
                // Double chest - spawn shulkers on both blocks
                BlockPos otherPos;
                if (chestType == ChestType.RIGHT) {
                    otherPos = pos.relative(facing.getCounterClockWise());
                } else {
                    otherPos = pos.relative(facing.getClockWise());
                }

                spawnShulkerMarker(world, pos, durationTicks, color);
                spawnShulkerMarker(world, otherPos, durationTicks, color);
                return;
            }
        }

        // Single chest or non-chest container
        spawnShulkerMarker(world, pos, durationTicks, color);
    }

    private static void spawnShulkerMarker(ServerLevel world, BlockPos pos, int durationTicks, ChatFormatting color) {
        Shulker marker = EntityHelper.create(EntityType.SHULKER, world);
        if (marker == null) return;

        // Position at center of block
        marker.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);

        // Make invisible but glowing
        marker.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, durationTicks, 0, false, false));
        marker.setGlowingTag(true);

        // Disable AI and make invulnerable
        marker.setNoAi(true);
        marker.setSilent(true);
        marker.setInvulnerable(true);
        marker.setNoGravity(true);

        // Scale down to 0.85 so clicks at edges hit the chest
        marker.getAttribute(Attributes.SCALE).setBaseValue(SHULKER_SCALE);

        // Set team color for the glow outline
        applyTeamColor(world, marker, color);

        world.addFreshEntity(marker);
        activeHighlights.put(marker.getUUID(), new HighlightEntry(world.dimension(), durationTicks));
    }

    private static void applyTeamColor(ServerLevel world, Entity marker, ChatFormatting color) {
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
    }

    /**
     * Called every server tick to manage highlight entity lifetime.
     */
    public static void tick(MinecraftServer server) {
        processHighlights(server, false);
    }

    /**
     * Clears all active highlights and discards their entities. Called on server stop.
     */
    public static void clearAll(MinecraftServer server) {
        processHighlights(server, true);
    }

    private static void processHighlights(MinecraftServer server, boolean forceRemoveAll) {
        if (activeHighlights.isEmpty()) return;

        Iterator<Map.Entry<UUID, HighlightEntry>> it = activeHighlights.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, HighlightEntry> entry = it.next();
            HighlightEntry info = entry.getValue();
            int remaining = info.ticksRemaining() - 1;

            if (forceRemoveAll || remaining <= 0) {
                ServerLevel level = server.getLevel(info.dimension());
                if (level != null) {
                    Entity entity = level.getEntity(entry.getKey());
                    if (entity != null) {
                        entity.discard();
                    }
                }
                it.remove();
            } else {
                entry.setValue(new HighlightEntry(info.dimension(), remaining));
            }
        }
    }
}

