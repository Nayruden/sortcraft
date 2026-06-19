package net.sortcraft.compat;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.scores.PlayerTeam;

/**
 * Minecraft 26.1.x implementation of the highlight version-compat seam used by
 * {@code ChestHighlighter}. On 26.1.x the stained glass blocks are exposed as
 * individual {@code Blocks} constants, entity type constants live on
 * {@code EntityType}, and {@code PlayerTeam#setColor} takes a {@link ChatFormatting}.
 * See the 26.2 implementation for the API differences.
 */
public final class HighlightCompat {
    private HighlightCompat() {}

    /** Returns the stained glass block state for the given dye color. */
    public static BlockState glassForDye(DyeColor dye) {
        Block block = switch (dye) {
            case WHITE -> Blocks.WHITE_STAINED_GLASS;
            case ORANGE -> Blocks.ORANGE_STAINED_GLASS;
            case MAGENTA -> Blocks.MAGENTA_STAINED_GLASS;
            case LIGHT_BLUE -> Blocks.LIGHT_BLUE_STAINED_GLASS;
            case YELLOW -> Blocks.YELLOW_STAINED_GLASS;
            case LIME -> Blocks.LIME_STAINED_GLASS;
            case PINK -> Blocks.PINK_STAINED_GLASS;
            case GRAY -> Blocks.GRAY_STAINED_GLASS;
            case LIGHT_GRAY -> Blocks.LIGHT_GRAY_STAINED_GLASS;
            case CYAN -> Blocks.CYAN_STAINED_GLASS;
            case PURPLE -> Blocks.PURPLE_STAINED_GLASS;
            case BLUE -> Blocks.BLUE_STAINED_GLASS;
            case BROWN -> Blocks.BROWN_STAINED_GLASS;
            case GREEN -> Blocks.GREEN_STAINED_GLASS;
            case RED -> Blocks.RED_STAINED_GLASS;
            case BLACK -> Blocks.BLACK_STAINED_GLASS;
        };
        return block.defaultBlockState();
    }

    /** Creates an (unspawned) block display entity used as the highlight marker. */
    public static Display.BlockDisplay createBlockDisplay(ServerLevel world) {
        return EntityType.BLOCK_DISPLAY.create(world, EntitySpawnReason.COMMAND);
    }

    /** Sets the team color used for the glow outline. */
    public static void setTeamColor(PlayerTeam team, ChatFormatting color) {
        team.setColor(color);
    }
}
