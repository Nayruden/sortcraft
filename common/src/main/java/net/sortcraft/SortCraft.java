package net.sortcraft;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.sortcraft.audit.SortAuditLogger;
import net.sortcraft.category.CategoryLoader;
import net.sortcraft.command.CommandHandler;
import net.sortcraft.command.SortInputCommand;
import net.sortcraft.config.ConfigManager;
import net.sortcraft.highlight.ChestHighlighter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SortCraft mod entry point.
 * Provides handler methods for platform-specific event registration.
 *
 * <p>Event registration is performed by each platform's entry point:
 * <ul>
 *   <li>Fabric: {@code SortCraftFabric} uses Fabric API events</li>
 *   <li>NeoForge: {@code SortCraftNeoForge} uses NeoForge event bus</li>
 * </ul>
 */
public class SortCraft {
    public static final String MODID = "sortcraft";
    private static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    /**
     * Called when the server is starting. Loads configuration and categories.
     */
    public static void onServerStarting(MinecraftServer server) {
        ConfigManager.loadConfig();
        CategoryLoader.loadCategories(server);
        CategoryLoader.flattenCategories();
    }

    /**
     * Called when the server is stopping. Cleans up static state.
     */
    public static void onServerStopping(MinecraftServer server) {
        SortAuditLogger.shutdown();
        CategoryLoader.clear();
        ChestHighlighter.clearAll(server);
        LOGGER.debug("Server stopping - cleared SortCraft static state");
    }

    /**
     * Called on server post-tick. Manages highlight entity lifetime.
     */
    public static void onServerTick(MinecraftServer server) {
        ChestHighlighter.tick(server);
    }

    /**
     * Registers commands with the dispatcher.
     */
    public static void onRegisterCommands(com.mojang.brigadier.CommandDispatcher<CommandSourceStack> dispatcher) {
        CommandHandler.register(dispatcher);
    }

    /**
     * Handles right-click block interaction for [input] sign sorting.
     *
     * @return true if the event was handled (sign click triggered sort), false to pass
     */
    public static boolean onRightClickBlock(Player player, InteractionHand hand, BlockPos pos) {
        if (!(player instanceof ServerPlayer serverPlayer)) return false;
        if (hand != InteractionHand.MAIN_HAND) return false;

        ServerLevel world = (ServerLevel) serverPlayer.level();
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof WallSignBlock)) return false;

        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof SignBlockEntity signBe)) return false;

        if (findTextOnSign(signBe, CommandHandler.getInputSignText()) != null) {
            CommandSourceStack source = serverPlayer.createCommandSourceStack();
            boolean isPreview = serverPlayer.isShiftKeyDown();
            try {
                SortInputCommand.execute(source, isPreview);
            } catch (Exception e) {
                LOGGER.error("Error executing sort from sign click", e);
            }
            return true;
        }

        return false;
    }

    /**
     * Finds text on a sign (case-insensitive).
     */
    private static String findTextOnSign(SignBlockEntity sign, String text) {
        String normalizedText = text.toLowerCase();

        for (int i = 0; i < 4; i++) {
            String frontLine = sign.getFrontText().getMessage(i, false).getString().trim().toLowerCase();
            if (frontLine.contains(normalizedText)) return frontLine;
        }
        return null;
    }
}
