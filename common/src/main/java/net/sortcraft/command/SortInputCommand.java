package net.sortcraft.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.sortcraft.audit.SortAuditLog;
import net.sortcraft.audit.SortAuditLogger;
import net.sortcraft.config.ConfigManager;
import net.sortcraft.container.ChestRef;
import net.sortcraft.container.ContainerHelper;
import net.sortcraft.container.SortContext;
import net.sortcraft.highlight.ChestHighlighter;
import net.sortcraft.sorting.SortingEngine;
import net.sortcraft.sorting.SortingResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Handles /sort input and /sort preview commands.
 */
public final class SortInputCommand {
    private SortInputCommand() {}

    private static final Logger LOGGER = LoggerFactory.getLogger("sortraft");
    private static final int INPUT_SIGN_SEARCH_RADIUS = 20;

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

        BlockPos chestPos = ContainerHelper.getAttachedContainerPos(inputSign.getBlockPos(), inputSign.getBlockState(), world);
        LOGGER.debug("[sortinput] Attached container position resolved: {}", chestPos);

        if (chestPos == null) {
            source.sendSuccess(() -> Component.literal("Input sign isn't attached to a container."), false);
            LOGGER.debug("[sortinput] Sign at {} is not attached to a container.", inputSign.getBlockPos());
            return 0;
        }

        // Collect all containers in the input stack (starts from sign's container and goes downward)
        List<ChestRef> inputChests = ContainerHelper.collectContainerStack(world, chestPos);
        if (inputChests.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Could not access input chest inventory."), false);
            LOGGER.debug("[sortinput] Failed to access chest stack starting at {}", chestPos);
            return 0;
        }

        LOGGER.debug("[sortinput] Input chest stack loaded: {} chest(s). Beginning sort.", inputChests.size());

        // Start audit logging if enabled (use the top chest position for audit)
        SortAuditLog audit = SortAuditLogger.isEnabled() && (!preview || SortAuditLogger.shouldLogPreviews())
                ? SortAuditLog.start(player, world, chestPos, searchRadius, preview)
                : null;

        SortingResults results = SortingEngine.sortFromContainers(context, world, inputChests, preview, audit);

        // Complete and log the audit entry
        if (audit != null) {
            audit.completeAndLog(results);
        }

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

                // Highlight destination chests for preview
                highlightPreviewChests(player, world, context, counts.keySet(), results.overflowCategories);
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

    /**
     * Highlights destination chests for preview mode.
     * Clears any previous preview highlights for the player, then highlights
     * category chests with color coding (AQUA for success, RED for overflow).
     * Respects MAX_PREVIEW_HIGHLIGHTS limit and deduplicates positions.
     *
     * @param player The player triggering the preview
     * @param world The server level
     * @param context The sort context with cached sign positions
     * @param categories Categories that will receive items
     * @param overflowCategories Categories that are full (overflow)
     */
    private static void highlightPreviewChests(ServerPlayer player, ServerLevel world, SortContext context,
                                                Set<String> categories, Set<String> overflowCategories) {
        MinecraftServer server = world.getServer();
        UUID playerUUID = player.getUUID();

        // Clear any previous preview highlights for this player
        ChestHighlighter.clearPlayerHighlights(server, playerUUID);

        // Track already-highlighted positions to avoid duplicates
        Set<BlockPos> highlightedPositions = new HashSet<>();
        int highlightCount = 0;

        // First pass: highlight successful destinations (AQUA)
        for (String category : categories) {
            if (highlightCount >= ChestHighlighter.MAX_PREVIEW_HIGHLIGHTS) break;
            if (overflowCategories.contains(category)) continue; // Handle overflow separately

            List<ChestRef> chests = SortingEngine.findCategoryChests(context, world, category);
            if (chests.isEmpty()) continue;

            // Only highlight the first chest in the stack (not entire stack)
            BlockPos pos = chests.get(0).getPos();
            if (highlightedPositions.contains(pos)) continue;

            highlightedPositions.add(pos);
            ChestHighlighter.highlightChestForPlayer(world, pos,
                    ChestHighlighter.PREVIEW_DURATION_TICKS, ChatFormatting.AQUA, playerUUID);
            highlightCount++;
        }

        // Second pass: highlight overflow destinations (RED)
        for (String category : overflowCategories) {
            if (highlightCount >= ChestHighlighter.MAX_PREVIEW_HIGHLIGHTS) break;

            List<ChestRef> chests = SortingEngine.findCategoryChests(context, world, category);
            if (chests.isEmpty()) continue;

            // Only highlight the first chest in the stack
            BlockPos pos = chests.get(0).getPos();
            if (highlightedPositions.contains(pos)) continue;

            highlightedPositions.add(pos);
            ChestHighlighter.highlightChestForPlayer(world, pos,
                    ChestHighlighter.PREVIEW_DURATION_TICKS, ChatFormatting.RED, playerUUID);
            highlightCount++;
        }

        // Notify if we hit the limit
        if (categories.size() + overflowCategories.size() > ChestHighlighter.MAX_PREVIEW_HIGHLIGHTS) {
            int skipped = (categories.size() + overflowCategories.size()) - highlightCount;
            player.sendSystemMessage(Component.literal(
                    "§7(Showing " + highlightCount + " of " + (categories.size() + overflowCategories.size()) +
                    " destination categories - " + skipped + " not highlighted)"));
        }
    }

}

