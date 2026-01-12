package net.sortcraft.highlight;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.sortcraft.compat.EntityHelper;
import net.sortcraft.mixin.accessor.BlockDisplayAccessor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages glowing chest highlights using block_display entities.
 * Block displays render a stained glass block with a glowing outline visible through walls.
 * Unlike Shulkers, block_display entities have no hitbox, allowing normal chest interaction.
 * For double chests, spawns a display on each block.
 *
 * Supports per-player highlight tracking for preview mode, allowing
 * previous highlights to be cleared before spawning new ones.
 */
public final class ChestHighlighter {
    private ChestHighlighter() {}

    /** Maximum number of chests to highlight in preview mode to avoid entity spam. */
    public static final int MAX_PREVIEW_HIGHLIGHTS = 10;

    /** Duration for preview highlights (shorter than whereis). */
    public static final int PREVIEW_DURATION_TICKS = 5 * 20; // 5 seconds

    /** Tracks an active highlight entity with optional player association. */
    private record HighlightEntry(ResourceKey<Level> dimension, int ticksRemaining, UUID playerUUID) {}

    // Track active chest highlights: entity UUID -> highlight info
    private static final Map<UUID, HighlightEntry> activeHighlights = new ConcurrentHashMap<>();

    // Track highlights per player for preview mode: player UUID -> set of entity UUIDs
    private static final Map<UUID, Set<UUID>> playerHighlights = new ConcurrentHashMap<>();

    /**
     * Highlights a chest (or double chest) with a glowing outline visible through walls.
     * For double chests, spawns a block_display on each block position.
     * This version is not associated with any player (used by whereis command).
     */
    public static void highlightChest(ServerLevel world, BlockPos pos, int durationTicks, ChatFormatting color) {
        highlightChestInternal(world, pos, durationTicks, color, null);
    }

    /**
     * Highlights a chest for a specific player, allowing the highlight to be cleared
     * when that player triggers a new preview. Used for preview mode highlighting.
     *
     * @param world The server level
     * @param pos The chest position
     * @param durationTicks How long the highlight should last
     * @param color The glow color
     * @param playerUUID The player who triggered this highlight (for tracking)
     */
    public static void highlightChestForPlayer(ServerLevel world, BlockPos pos, int durationTicks,
                                                ChatFormatting color, UUID playerUUID) {
        highlightChestInternal(world, pos, durationTicks, color, playerUUID);
    }

    private static void highlightChestInternal(ServerLevel world, BlockPos pos, int durationTicks,
                                                ChatFormatting color, UUID playerUUID) {
        BlockState state = world.getBlockState(pos);

        // Check if this is a double chest and highlight both blocks
        if (state.getBlock() instanceof ChestBlock) {
            ChestType chestType = state.getValue(BlockStateProperties.CHEST_TYPE);
            Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);

            if (chestType != ChestType.SINGLE) {
                // Double chest - spawn displays on both blocks
                BlockPos otherPos;
                if (chestType == ChestType.RIGHT) {
                    otherPos = pos.relative(facing.getCounterClockWise());
                } else {
                    otherPos = pos.relative(facing.getClockWise());
                }

                spawnBlockDisplayMarker(world, pos, durationTicks, color, playerUUID);
                spawnBlockDisplayMarker(world, otherPos, durationTicks, color, playerUUID);
                return;
            }
        }

        // Single chest or non-chest container
        spawnBlockDisplayMarker(world, pos, durationTicks, color, playerUUID);
    }

    /**
     * Maps ChatFormatting colors to corresponding stained glass blocks.
     */
    private static BlockState getGlassForColor(ChatFormatting color) {
        if (color == null) {
            return Blocks.WHITE_STAINED_GLASS.defaultBlockState();
        }
        return switch (color) {
            case AQUA -> Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
            case RED -> Blocks.RED_STAINED_GLASS.defaultBlockState();
            case YELLOW -> Blocks.YELLOW_STAINED_GLASS.defaultBlockState();
            case GREEN -> Blocks.LIME_STAINED_GLASS.defaultBlockState();
            case BLUE -> Blocks.BLUE_STAINED_GLASS.defaultBlockState();
            case LIGHT_PURPLE -> Blocks.MAGENTA_STAINED_GLASS.defaultBlockState();
            case DARK_PURPLE -> Blocks.PURPLE_STAINED_GLASS.defaultBlockState();
            case GOLD -> Blocks.ORANGE_STAINED_GLASS.defaultBlockState();
            case GRAY -> Blocks.GRAY_STAINED_GLASS.defaultBlockState();
            case DARK_GRAY -> Blocks.GRAY_STAINED_GLASS.defaultBlockState();
            case DARK_AQUA -> Blocks.CYAN_STAINED_GLASS.defaultBlockState();
            case DARK_GREEN -> Blocks.GREEN_STAINED_GLASS.defaultBlockState();
            case DARK_RED -> Blocks.RED_STAINED_GLASS.defaultBlockState();
            case DARK_BLUE -> Blocks.BLUE_STAINED_GLASS.defaultBlockState();
            case BLACK -> Blocks.BLACK_STAINED_GLASS.defaultBlockState();
            case WHITE -> Blocks.WHITE_STAINED_GLASS.defaultBlockState();
            default -> Blocks.WHITE_STAINED_GLASS.defaultBlockState();
        };
    }

    private static void spawnBlockDisplayMarker(ServerLevel world, BlockPos pos, int durationTicks,
                                                 ChatFormatting color, UUID playerUUID) {
        Display.BlockDisplay marker = EntityHelper.create(EntityType.BLOCK_DISPLAY, world);
        if (marker == null) return;

        // Position at the block's corner (block_display uses corner positioning)
        marker.setPos(pos.getX(), pos.getY(), pos.getZ());

        // Set the block state to display (stained glass matching the color)
        ((BlockDisplayAccessor) marker).invokeSetBlockState(getGlassForColor(color));

        // Make it glow
        marker.setGlowingTag(true);

        // Set team color for the glow outline
        applyTeamColor(world, marker, color);

        world.addFreshEntity(marker);

        UUID entityUUID = marker.getUUID();
        activeHighlights.put(entityUUID, new HighlightEntry(world.dimension(), durationTicks, playerUUID));

        // Track per-player if a player UUID was provided
        if (playerUUID != null) {
            playerHighlights.computeIfAbsent(playerUUID, k -> ConcurrentHashMap.newKeySet()).add(entityUUID);
        }
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

    /**
     * Clears all highlights associated with a specific player.
     * Call this before spawning new preview highlights for the same player
     * to prevent entity stacking from rapid clicks.
     *
     * @param server The Minecraft server
     * @param playerUUID The UUID of the player whose highlights should be cleared
     */
    public static void clearPlayerHighlights(MinecraftServer server, UUID playerUUID) {
        Set<UUID> entityUUIDs = playerHighlights.remove(playerUUID);
        if (entityUUIDs == null || entityUUIDs.isEmpty()) return;

        for (UUID entityUUID : entityUUIDs) {
            HighlightEntry info = activeHighlights.remove(entityUUID);
            if (info == null) continue;

            ServerLevel level = server.getLevel(info.dimension());
            if (level != null) {
                Entity entity = level.getEntity(entityUUID);
                if (entity != null) {
                    entity.discard();
                }
            }
        }
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
                // Also remove from player tracking if applicable
                if (info.playerUUID() != null) {
                    Set<UUID> playerSet = playerHighlights.get(info.playerUUID());
                    if (playerSet != null) {
                        playerSet.remove(entry.getKey());
                        if (playerSet.isEmpty()) {
                            playerHighlights.remove(info.playerUUID());
                        }
                    }
                }
                it.remove();
            } else {
                entry.setValue(new HighlightEntry(info.dimension(), remaining, info.playerUUID()));
            }
        }
    }
}

