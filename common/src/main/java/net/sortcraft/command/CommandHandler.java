package net.sortcraft.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.sortcraft.category.CategorySet;
import net.sortcraft.category.ShareConfigManager;
import net.sortcraft.compat.PermissionHelper;

/**
 * Registers all /sort subcommands.
 */
public final class CommandHandler {
    private CommandHandler() {}

    private static final String SIGN_PREFIX = "[";
    private static final String SIGN_SUFFIX = "]";
    private static final String INPUT_SIGN_TEXT = SIGN_PREFIX + "input" + SIGN_SUFFIX;

    public static String getInputSignText() {
        return INPUT_SIGN_TEXT;
    }

    public static String formatSignText(String text) {
        return SIGN_PREFIX + text + SIGN_SUFFIX;
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("sort")
                        .then(Commands.literal("input")
                                .executes(ctx -> SortInputCommand.execute(ctx.getSource(), false)))
                        .then(Commands.literal("preview")
                                .executes(ctx -> SortInputCommand.execute(ctx.getSource(), true)))
                        .then(Commands.literal("diagnostics")
                                .requires(source -> PermissionHelper.hasOpLevel(source, 2))
                                .executes(DiagCommand::execute))
                        .then(Commands.literal("whereis")
                                .then(Commands.argument("item", StringArgumentType.greedyString())
                                        .suggests((context, builder) ->
                                                SharedSuggestionProvider.suggestResource(BuiltInRegistries.ITEM.keySet(), builder))
                                        .executes(WhereIsCommand::execute)))
                        .then(Commands.literal("category")
                                .then(Commands.argument("item", StringArgumentType.greedyString())
                                        .suggests((context, builder) ->
                                                SharedSuggestionProvider.suggestResource(BuiltInRegistries.ITEM.keySet(), builder))
                                        .executes(CategoryCommand::execute)))
                        .then(Commands.literal("help")
                                .executes(CommandHandler::executeHelp))
                        .then(Commands.literal("dump")
                                .requires(source -> PermissionHelper.hasOpLevel(source, 2))
                                .executes(DumpCommand::execute))
                        .then(Commands.literal("reload")
                                .requires(source -> PermissionHelper.hasOpLevel(source, 2))
                                .executes(ReloadCommand::execute))
                        .then(Commands.literal("shareconfig")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(CommandHandler::executeShareConfig)))
        );
    }

    private static int executeHelp(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) {
        String helpMessage = String.format("""
                Sort Command Help:
                /sort input           - Sorts items from the closest input chest (chest must have a sign with '%s')
                /sort preview         - Shows a preview of what will be sorted and where
                /sort diagnostics     - Generates a diagnostics report as YAML
                /sort whereis <item>  - Finds chests that contain the specified item
                /sort category <item> - Shows the sorting category for the specified item
                /sort shareconfig <id> - Tests a CategoryCraft share config by ID
                /sort reload          - Reloads category configurations from config files
                /sort dump            - Generates JSON files with all item tags from the registry
                /sort help            - Shows this help message

                All commands support autocomplete. Use TAB for suggestions.
                """, INPUT_SIGN_TEXT);
        context.getSource().sendSuccess(() -> Component.literal(helpMessage), false);
        return 1;
    }

    private static int executeShareConfig(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) {
        String shareId = StringArgumentType.getString(context, "id");
        CommandSourceStack source = context.getSource();

        if (!ShareConfigManager.isValidShareId(shareId)) {
            source.sendFailure(Component.literal("Invalid share ID format. Expected 8 characters: letters, digits, hyphens, underscores."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Resolving share config '" + shareId + "'...").withStyle(ChatFormatting.GRAY).copy(), false);
        CategorySet categorySet = ShareConfigManager.resolve(shareId);
        if (categorySet == null) {
            source.sendFailure(Component.literal("Failed to resolve share config '" + shareId + "'. Check server logs for details."));
            return 0;
        }

        int categoryCount = categorySet.getCategories().size();
        int itemCount = categorySet.getItemCategoryMap().size();
        source.sendSuccess(() -> Component.literal("Share config '" + shareId + "' loaded: " +
                categoryCount + " categories, " + itemCount + " unique items").withStyle(ChatFormatting.GREEN).copy(), false);
        return 1;
    }
}

