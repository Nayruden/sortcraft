package net.sortcraft.command;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.sortcraft.category.CategoryLoader;
import net.sortcraft.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles /sort reload command.
 */
public final class ReloadCommand {
    private ReloadCommand() {}

    private static final Logger LOGGER = LoggerFactory.getLogger("sortcraft");

    public static int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();

        try {
            ConfigManager.loadConfig();
            CategoryLoader.clear();
            CategoryLoader.loadCategories(server);
            CategoryLoader.flattenCategories();

            source.sendSuccess(() -> Component.literal("SortCraft configuration reloaded successfully."), false);
            LOGGER.info("[sortreload] Configuration reloaded successfully.");
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error reloading configuration: " + e.getMessage()));
            LOGGER.error("[sortreload] Failed to reload configuration.", e);
            return 0;
        }
    }
}

