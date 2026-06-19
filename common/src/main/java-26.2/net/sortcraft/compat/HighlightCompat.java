package net.sortcraft.compat;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.TeamColor;

import java.util.Optional;

/**
 * Minecraft 26.2+ implementation of the highlight version-compat seam used by
 * {@code ChestHighlighter}. Compared with earlier 26.x versions, 26.2:
 * <ul>
 *   <li>consolidated the per-color stained glass blocks into a single
 *       {@code ColorCollection} ({@link Blocks#STAINED_GLASS}) keyed by {@link DyeColor};</li>
 *   <li>moved entity type constants from {@code EntityType} to {@link EntityTypes};</li>
 *   <li>changed {@code PlayerTeam#setColor} to take an {@code Optional<TeamColor>}
 *       instead of a {@link ChatFormatting}.</li>
 * </ul>
 */
public final class HighlightCompat {
    private HighlightCompat() {}

    /** Returns the stained glass block state for the given dye color. */
    public static BlockState glassForDye(DyeColor dye) {
        return Blocks.STAINED_GLASS.pick(dye).defaultBlockState();
    }

    /** Creates an (unspawned) block display entity used as the highlight marker. */
    public static Display.BlockDisplay createBlockDisplay(ServerLevel world) {
        return EntityTypes.BLOCK_DISPLAY.create(world, EntitySpawnReason.COMMAND);
    }

    /** Sets the team color used for the glow outline. */
    public static void setTeamColor(PlayerTeam team, ChatFormatting color) {
        team.setColor(teamColorForFormatting(color));
    }

    private static Optional<TeamColor> teamColorForFormatting(ChatFormatting color) {
        if (color == null) {
            return Optional.empty();
        }
        try {
            // TeamColor shares constant names with the 16 ChatFormatting colors.
            return Optional.of(TeamColor.valueOf(color.name()));
        } catch (IllegalArgumentException e) {
            // Not a color formatting (e.g. a style code) — no outline color.
            return Optional.empty();
        }
    }
}
