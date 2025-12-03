package net.sortcraft;

import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.sortcraft.category.CategoryLoader;
import net.sortcraft.command.CommandHandler;
import net.sortcraft.command.SortInputCommand;
import net.sortcraft.compat.EventHelper;
import net.sortcraft.config.ConfigManager;
import net.sortcraft.highlight.ChestHighlighter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SortCraft mod entry point.
 * Handles initialization and event registration.
 */
public class SortCraft {
    public static final String MODID = "sortcraft";
    private static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public static void init() {
        // Server starting: load configuration and categories
        LifecycleEvent.SERVER_STARTING.register(server -> {
            ConfigManager.loadConfig();
            CategoryLoader.loadCategories(server);
            CategoryLoader.flattenCategories();
        });

        // Server stopping: clean up static state (important for integrated server / single-player)
        LifecycleEvent.SERVER_STOPPING.register(server -> {
            CategoryLoader.clear();
            ChestHighlighter.clearAll();
            LOGGER.debug("Server stopping - cleared SortCraft static state");
        });

        // Tick handler to manage highlight entity lifetime
        TickEvent.SERVER_POST.register(ChestHighlighter::tick);

        // Register commands
        CommandRegistrationEvent.EVENT.register((dispatcher, registryAccess, environment) -> {
            CommandHandler.register(dispatcher);
        });

        // Right-click on [input] sign triggers sorting
        InteractionEvent.RIGHT_CLICK_BLOCK.register((player, hand, pos, direction) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return EventHelper.pass();
            if (hand != InteractionHand.MAIN_HAND) return EventHelper.pass();

            ServerLevel world = (ServerLevel) serverPlayer.level();
            BlockState state = world.getBlockState(pos);
            if (!(state.getBlock() instanceof WallSignBlock)) return EventHelper.pass();

            BlockEntity be = world.getBlockEntity(pos);
            if (!(be instanceof SignBlockEntity signBe)) return EventHelper.pass();

            if (findTextOnSign(signBe, CommandHandler.getInputSignText()) != null) {
                CommandSourceStack source = serverPlayer.createCommandSourceStack();
                try {
                    SortInputCommand.execute(source, false);
                } catch (Exception e) {
                    LOGGER.error("Error executing sort from sign click", e);
                }
                return EventHelper.success();
            }

            return EventHelper.pass();
        });
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
