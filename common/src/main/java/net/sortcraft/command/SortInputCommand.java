package net.sortcraft.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.sortcraft.config.ConfigManager;
import net.sortcraft.container.ContainerHelper;
import net.sortcraft.container.SortContext;
import net.sortcraft.highlight.ChestHighlighter;
import net.sortcraft.sorting.SortingEngine;
import net.sortcraft.sorting.SortingResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Handles /sort input and /sort preview commands.
 */
public final class SortInputCommand {
    private SortInputCommand() {}

    private static final Logger LOGGER = LoggerFactory.getLogger("sortcraft");
    private static final int INPUT_SIGN_SEARCH_RADIUS = 20;
    private static final int HIGHLIGHT_DURATION_TICKS = 3 * 20; // 3 seconds

    public static int execute(CommandSourceStack source, boolean preview) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be run by a player."));
            return 0;
        }
        ServerLevel world = source.getLevel();
        BlockPos playerPos = player.blockPosition();
        LOGGER.debug("[sortinput] Starting sort near {}", playerPos);

        int searchRadius = ConfigManager.getSearchRadius();
        SortContext context = new SortContext(world, playerPos, Math.max(searchRadius, INPUT_SIGN_SEARCH_RADIUS));

        SignBlockEntity inputSign = context.findSign(CommandHandler.getInputSignText());

        if (inputSign == null) {
            source.sendSuccess(() -> Component.literal("No input sign found nearby."), false);
            LOGGER.debug("[sortinput] No input sign found within search radius.");
            return 0;
        }

        BlockPos chestPos = ContainerHelper.getAttachedChestPos(inputSign.getBlockPos(), inputSign.getBlockState(), world);
        LOGGER.debug("[sortinput] Attached chest position resolved: {}", chestPos);

        if (chestPos == null) {
            source.sendSuccess(() -> Component.literal("Input sign isn't attached to a chest."), false);
            LOGGER.debug("[sortinput] Sign at {} is not attached to a chest.", inputSign.getBlockPos());
            return 0;
        }

        BlockState state = world.getBlockState(chestPos);
        if (!(state.getBlock() instanceof ChestBlock chestBlock)) {
            source.sendSuccess(() -> Component.literal("Block attached to the input sign is not a chest."), false);
            LOGGER.debug("[sortinput] Block at {} is not a ChestBlock.", chestPos);
            return 0;
        }

        Container inputInv = ChestBlock.getContainer(chestBlock, state, world, chestPos, true);
        if (inputInv == null) {
            source.sendSuccess(() -> Component.literal("Could not access input chest inventory."), false);
            LOGGER.debug("[sortinput] Failed to access chest inventory at {}", chestPos);
            return 0;
        }

        LOGGER.debug("[sortinput] Input chest inventory loaded. Beginning sort.");

        SortingResults results = SortingEngine.sortStacks(context, world, SortingEngine.containerToIterable(inputInv), preview);

        StringBuilder message = new StringBuilder();
        message.append(SortingEngine.summarize(results.overflowCategories, "⚠ Storage overflow in following categories:"));
        message.append(SortingEngine.summarize(results.unknownItems, "⚠ No category found for following items:"));
        if (!message.isEmpty()) {
            final String messageStr = message.toString();
            source.sendSuccess(() -> Component.literal(messageStr), false);
        }

        if (preview) {
            Map<String, Integer> counts = results.categoryCounts;
            if (counts.isEmpty()) {
                source.sendSuccess(() -> Component.literal("No items to sort."), false);
            } else {
                source.sendSuccess(() -> Component.literal("Sort Preview:"), false);
                for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                    String cat = entry.getKey();
                    int count = entry.getValue();
                    source.sendSuccess(() -> Component.literal("- " + cat + ": " + count + " item" + (count != 1 ? "s" : "")), false);
                }
            }
            return 1;
        }

        if (results.sorted > 0) {
            String totalSortedStr = Integer.toString(results.sorted);
            source.sendSuccess(() -> Component.literal(totalSortedStr + " items sorted successfully."), false);
            LOGGER.debug("[sortinput] Sorting complete. {} total items sorted.", totalSortedStr);
        } else {
            source.sendSuccess(() -> Component.literal("No items were sorted."), false);
            LOGGER.debug("[sortinput] No items were sorted.");
        }

        return 1;
    }
}

