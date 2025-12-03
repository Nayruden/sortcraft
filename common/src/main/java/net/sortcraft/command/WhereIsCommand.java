package net.sortcraft.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.sortcraft.compat.RegistryHelper;
import net.sortcraft.config.ConfigManager;
import net.sortcraft.container.ContainerHelper;
import net.sortcraft.container.SortContext;
import net.sortcraft.highlight.ChestHighlighter;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handles /sort whereis command.
 */
public final class WhereIsCommand {
    private WhereIsCommand() {}

    private static final int HIGHLIGHT_DURATION_TICKS = 10 * 20; // 10 seconds

    public static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be run by a player."));
            return 0;
        }
        ServerLevel world = source.getLevel();
        BlockPos playerPos = player.blockPosition();
        String itemName = StringArgumentType.getString(context, "item");

        ResourceLocation itemId = ResourceLocation.tryParse(itemName);
        if (itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId)) {
            source.sendFailure(Component.literal("Unknown item: " + itemName));
            return 0;
        }

        Item item = RegistryHelper.getItemByKey(itemId);

        SortContext sortContext = new SortContext(world, playerPos, ConfigManager.getSearchRadius());
        sortContext.buildContainerCache();
        Map<BlockPos, Container> containerCache = sortContext.getContainerCache();

        if (containerCache.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No nearby containers found."), false);
            return 1;
        }

        Map<BlockPos, Iterable<ItemStack>> foundStorage = new HashMap<>();
        for (Map.Entry<BlockPos, Container> entry : containerCache.entrySet()) {
            foundStorage.put(entry.getKey(), containerToIterable(entry.getValue()));
        }

        List<BlockPos> validPositions = foundStorage.entrySet().stream()
                .filter(entry -> stacksContainsItem(entry.getValue(), item))
                .sorted(Comparator.comparingDouble(entry -> entry.getKey().distSqr(playerPos)))
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (validPositions.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No " + itemName + " found in nearby containers."), false);
            return 1;
        }

        source.sendSuccess(() -> Component.literal("Item '" + itemName + "' found in:"), false);

        for (BlockPos pos : validPositions) {
            source.sendSuccess(() -> Component.literal("- " + pos.toShortString()), false);
            ChestHighlighter.highlightChest(world, pos, HIGHLIGHT_DURATION_TICKS, ChatFormatting.YELLOW);
        }

        return 1;
    }

    private static boolean stacksContainsItem(Iterable<ItemStack> stacks, Item item) {
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;
            if (stack.getItem() == item) return true;

            Iterable<ItemStack> nested = ContainerHelper.getStacksIfContainer(stack);
            if (nested != null) {
                if (stacksContainsItem(nested, item)) return true;
            }
        }
        return false;
    }

    private static Iterable<ItemStack> containerToIterable(Container container) {
        return () -> new java.util.Iterator<>() {
            private int index = 0;
            private final int size = container.getContainerSize();

            @Override
            public boolean hasNext() {
                return index < size;
            }

            @Override
            public ItemStack next() {
                return container.getItem(index++);
            }
        };
    }
}

